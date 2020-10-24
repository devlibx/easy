package io.gitbub.harishb2k.easy.helper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LocalHttpServer {
    public volatile int port;
    private volatile boolean keepRunning;
    private final CountDownLatch waitForServerStartLatch;
    private final CountDownLatch waitForServerStopLatch;

    public LocalHttpServer() {
        waitForServerStartLatch = new CountDownLatch(1);
        waitForServerStopLatch = new CountDownLatch(1);
    }

    public static void main(String[] args) {
        LocalHttpServer localHttpServer = new LocalHttpServer();
        localHttpServer.startServer();
    }

    public void stopServer() {
        keepRunning = false;
        try {
            waitForServerStopLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public void startServerInThread() {
        new Thread(this::startServer).start();
        try {
            waitForServerStartLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public void startServer() {
        HttpServer server = null;
        for (port = 9123; port < 9130; port++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                System.out.println("Using Port = " + port);
                break;
            } catch (Exception e) {
                System.out.println("Port=" + port + " is not free, try next");
            }
        }
        if (server == null) {
            throw new RuntimeException("Could not run http server");
        }

        server.createContext("/delay", new DelayHttpHandler());
        // server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        waitForServerStartLatch.countDown();

        keepRunning = true;
        while (keepRunning) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
        waitForServerStopLatch.countDown();
    }

    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        // query = query.replace("/delay?", "");
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    private static class DelayHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) {
            try {
                Map<String, String> qp = splitQuery(t.getRequestURI().getQuery());

                int sleep = Integer.parseInt(qp.get("delay"));
                log.debug("HTTP Request - {} # Sleep for {} ms", t.getRequestURI(), sleep);
                Thread.sleep(sleep);

                Map<String, Object> data = new HashMap<>();
                data.put("data", "some data");
                data.putAll(qp);
                String response = new StringHelper().stringify(data);
                if (qp.containsKey("status")) {
                    t.sendResponseHeaders(Integer.parseInt(qp.get("status")), response.length());
                } else {
                    t.sendResponseHeaders(200, response.length());
                }
                t.getResponseHeaders().add("Content-Type", "application/json");
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                System.out.println("Got some error");
                throw new RuntimeException(e);
            }
        }
    }
}

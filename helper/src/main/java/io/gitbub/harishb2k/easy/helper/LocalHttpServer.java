package io.gitbub.harishb2k.easy.helper;

import com.google.common.base.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
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
                log.debug("Using port={} for test server", port);
                break;
            } catch (Exception e) {
                log.debug("Port= {} is not free, try next", port);
            }
        }
        if (server == null) {
            throw new RuntimeException("Could not run http server");
        }

        server.createContext("/delay", new DelayHttpHandler());
        // server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        waitForServerStartLatch.countDown();
        log.trace("HTTP Server at port {} started", port);

        keepRunning = true;
        while (keepRunning) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
        server.stop(1);
        log.trace("HTTP Server at port {} stopped", port);
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
            try (OutputStream os = t.getResponseBody(); InputStream in = t.getRequestBody()) {
                Map<String, String> qp = splitQuery(t.getRequestURI().getQuery());

                int sleep = Integer.parseInt(qp.get("delay"));
                log.debug("HTTP Request - {} # Sleep for {} ms", t.getRequestURI(), sleep);
                Thread.sleep(sleep);

                String requestBody = null;
                try {
                    requestBody = IOUtils.toString(in, Charset.defaultCharset());
                } catch (Exception e) {
                }

                Headers headers = t.getRequestHeaders();
                String headerString = null;
                if (headers != null) {
                    Map<String, Object> h = new HashMap<>();
                    headers.forEach(h::put);
                    headerString = JsonUtils.asJson(h);
                }

                String response = null;
                if ("GET".equals(t.getRequestMethod())) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("method", "get");
                    data.put("data", "some data");
                    if (!Strings.isNullOrEmpty(requestBody)) {
                        data.put("request_body", requestBody);
                    }
                    if (!Strings.isNullOrEmpty(headerString)) {
                        data.put("headers", headerString);
                    }
                    data.putAll(qp);
                    response = new StringHelper().stringify(data);
                } else if ("POST".equals(t.getRequestMethod())) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("method", "post");
                    data.put("data", "some data");
                    if (!Strings.isNullOrEmpty(requestBody)) {
                        data.put("request_body", requestBody);
                    }
                    if (!Strings.isNullOrEmpty(headerString)) {
                        data.put("headers", headerString);
                    }
                    data.putAll(qp);
                    response = new StringHelper().stringify(data);
                } else if ("PUT".equals(t.getRequestMethod())) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("method", "put");
                    data.put("data", "some data");
                    if (!Strings.isNullOrEmpty(requestBody)) {
                        data.put("request_body", requestBody);
                    }
                    if (!Strings.isNullOrEmpty(headerString)) {
                        data.put("headers", headerString);
                    }
                    data.putAll(qp);
                    response = new StringHelper().stringify(data);
                } else if ("DELETE".equals(t.getRequestMethod())) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("method", "delete");
                    data.put("data", "some data");
                    if (!Strings.isNullOrEmpty(requestBody)) {
                        data.put("request_body", requestBody);
                    }
                    if (!Strings.isNullOrEmpty(headerString)) {
                        data.put("headers", headerString);
                    }
                    data.putAll(qp);
                    response = new StringHelper().stringify(data);
                }

                if (qp.containsKey("status")) {
                    t.sendResponseHeaders(Integer.parseInt(qp.get("status")), response.length());
                } else {
                    t.sendResponseHeaders(200, response.length());
                }
                t.getResponseHeaders().add("Content-Type", "application/json");
                os.write(response.getBytes());
            } catch (Exception e) {
                if (e instanceof IOException) {
                    log.error("Got some IOException error in http server");
                } else {
                    log.error("Got some error in http server : {}", e.getMessage());
                }
            }
        }
    }
}

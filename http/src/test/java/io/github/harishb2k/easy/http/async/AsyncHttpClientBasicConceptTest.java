package io.github.harishb2k.easy.http.async;

import com.google.common.base.Strings;
import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.ParallelThread;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.github.harishb2k.easy.http.BaseTestCase;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncHttpClientBasicConceptTest extends BaseTestCase {
    private String service;
    private HttpClient httpClient;
    private WebClient webClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        LoggingHelper.setupLogging();
        service = UUID.randomUUID().toString();
        httpClient = HttpClient.create(ConnectionProvider.create(service, 10))
                .tcpConfiguration(tcpClient ->
                        tcpClient.doOnConnected(connection -> connection
                                .addHandlerLast(new ReadTimeoutHandler(1, TimeUnit.SECONDS))
                        )
                );
        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + localHttpServer.port)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public void testReactor_HttpClient_Get_Example_With_Success() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .get()
                .uri("/delay?delay=20")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    wait.countDown();
                    System.out.println("Got exception which was not expected - " + throwable);
                })
                .subscribe(data -> {
                    if (!Strings.isNullOrEmpty(data)) {
                        gotExpected.set(true);
                    }
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }

    public void testReactor_HttpClient_Get_Example_With_404() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .get()
                .uri("/invalid_api")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (throwable instanceof WebClientResponseException.NotFound) {
                        gotExpected.set(true);
                    } else {
                        System.out.println("Got " + throwable + " this is not expected exception");
                    }
                    wait.countDown();
                })
                .subscribe(data -> {
                    System.out.println("Got some response which was not expected - " + data);
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }

    public void testReactor_HttpClient_Get_Example_With_Timeout() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .get()
                .uri("/delay?delay=2000")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (throwable instanceof ReadTimeoutException) {
                        gotExpected.set(true);
                    } else {
                        System.out.println("Got " + throwable + " this is not expected exception");
                    }
                    wait.countDown();
                })
                .subscribe(data -> {
                    System.out.println("Got some response which was not expected - " + data);
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }


    public void testReactor_HttpClient_Post_Example_With_Success() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .post()
                .uri("/delay?delay=20")
                .bodyValue("my request body")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    wait.countDown();
                    System.out.println("Got exception which was not expected - " + throwable);
                })
                .subscribe(data -> {
                    if (!Strings.isNullOrEmpty(data)) {
                        Map<String, Object> dataMap = JsonUtils.convertAsMap(data);
                        assertEquals("post", dataMap.get("method"));
                        assertEquals("my request body", dataMap.get("request_body"));
                        gotExpected.set(true);
                    }
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }

    public void testReactor_HttpClient_Post_Example_With_404() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .post()
                .uri("/invalid_api?delay=20")
                .bodyValue("my request body")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (throwable instanceof WebClientResponseException.NotFound) {
                        gotExpected.set(true);
                    } else {
                        System.out.println("Got " + throwable + " this is not expected exception");
                    }
                    wait.countDown();
                })
                .subscribe(data -> {
                    System.out.println("Got some response which was not expected - " + data);
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }

    public void testReactor_HttpClient_Post_Example_With_Timeout() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .post()
                .uri("/delay?delay=2000")
                .bodyValue("my request body")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (throwable instanceof ReadTimeoutException) {
                        gotExpected.set(true);
                    } else {
                        System.out.println("Got " + throwable + " this is not expected exception");
                    }
                    wait.countDown();
                })
                .subscribe(data -> {
                    System.out.println("Got some response which was not expected - " + data);
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }


    public void testReactor_HttpClient_Put_Example_With_Success() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .put()
                .uri("/delay?delay=20")
                .bodyValue("my request body")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    wait.countDown();
                    System.out.println("Got exception which was not expected - " + throwable);
                })
                .subscribe(data -> {
                    if (!Strings.isNullOrEmpty(data)) {
                        Map<String, Object> dataMap = JsonUtils.convertAsMap(data);
                        assertEquals("put", dataMap.get("method"));
                        assertEquals("my request body", dataMap.get("request_body"));
                        gotExpected.set(true);
                    }
                    wait.countDown();
                });
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }

    public void testReactor_HttpClient_Delete_Example_With_Success() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotExpected = new AtomicBoolean(false);
        webClient
                .delete()
                .uri("/delay?delay=20")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    wait.countDown();
                    System.out.println("Got exception which was not expected - " + throwable);
                })
                .subscribe(data -> {
                    if (!Strings.isNullOrEmpty(data)) {
                        Map<String, Object> dataMap = JsonUtils.convertAsMap(data);
                        assertEquals("delete", dataMap.get("method"));
                        gotExpected.set(true);
                    }
                    wait.countDown();
                });
        wait.await(100, TimeUnit.SECONDS);
        assertTrue(gotExpected.get());
    }

    public void testParallelRequests_Get() throws Exception {
        Logger.getLogger(LocalHttpServer.class).setLevel(Level.INFO);

        httpClient = HttpClient.create(ConnectionProvider.create(service, 100))
                .tcpConfiguration(tcpClient ->
                        tcpClient.doOnConnected(connection -> connection
                                .addHandlerLast(new ReadTimeoutHandler(20, TimeUnit.SECONDS))
                        )
                );
        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + localHttpServer.port)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        int count = 100;
        AtomicInteger successCount = new AtomicInteger();
        CountDownLatch wait = new CountDownLatch(count);
        ParallelThread parallelThread = new ParallelThread(count, "testParallelRequests_Get");
        parallelThread.execute(() -> {
            webClient
                    .get()
                    .uri("/delay?delay=1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(throwable -> {
                        wait.countDown();
                        System.out.println("Got exception which was not expected - " + throwable);
                    })
                    .subscribe(data -> {
                        if (!Strings.isNullOrEmpty(data)) {
                            successCount.incrementAndGet();
                        }
                        wait.countDown();
                    });
        });
        wait.await(10, TimeUnit.SECONDS);

        // We must have got success on 95% calls
        assertTrue(successCount.get() > (count - 20));
    }
}
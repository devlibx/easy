package io.github.harishb2k.easy.http.async;

import com.google.common.base.Strings;
import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncHttpClientBasicConceptTest extends TestCase {
    private LocalHttpServer localHttpServer;
    private String service;
    private HttpClient httpClient;
    private WebClient webClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        localHttpServer = new LocalHttpServer();
        localHttpServer.startServerInThread();
        setupLogging();
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

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        localHttpServer.stopServer();
    }

    private void setupLogging() {
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(org.apache.log4j.Level.DEBUG);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        Logger.getLogger("io.github.harishb2k.easy.http.sync").setLevel(Level.OFF);
        Logger.getLogger(LocalHttpServer.class).setLevel(Level.DEBUG);
    }
}
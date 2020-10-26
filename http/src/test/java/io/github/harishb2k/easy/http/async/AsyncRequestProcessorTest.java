package io.github.harishb2k.easy.http.async;

import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.ParallelThread;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceOverflowException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.github.harishb2k.easy.http.util.Call;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.harishb2k.easy.http.util.EasyHttp.callAsync;

@Slf4j
public class AsyncRequestProcessorTest extends TestCase {
    private LocalHttpServer localHttpServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Setup logging
        LoggingHelper.setupLogging();

        // Start server
        localHttpServer = new LocalHttpServer();
        localHttpServer.startServerInThread();

        // Read config and setup EasyHttp
        Config config = YamlUtils.readYamlCamelCase("sync_processor_config.yaml", Config.class);
        config.getServers().get("testServer").setPort(localHttpServer.port);
        config.getApis().forEach((apiName, api) -> {
            api.setAsync(true);
        });

        EasyHttp.setup(config);
    }

    public void testSuccessGet() {
        int delay = 10;
        Map resultSync = EasyHttp.callSync(
                Call.builder(Map.class)
                        .withServerAndApi("testServer", "delay_timeout_5000")
                        .addQueryParam("delay", delay)
                        .build()
        );
        assertEquals(delay + "", resultSync.get("delay"));
        assertEquals("some data", resultSync.get("data"));
    }


    /**
     * Test a simple http call where we make too many calls to simulate requests rejected
     */
    public void testRequestTimeout() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotException = new AtomicBoolean(false);
        try {
            Map result = EasyHttp.callSync(
                    Call.builder(Map.class)
                            .withServerAndApi("testServer", "delay_timeout_10")
                            .addQueryParam("delay", 100)
                            .build()
            );
            wait.countDown();
        } catch (EasyResilienceRequestTimeoutException | EasyRequestTimeOutException e) {
            gotException.set(true);
            wait.countDown();
        } catch (Exception e) {
            System.out.println("Not expected - " + e);
            wait.countDown();
            e.printStackTrace();
        }
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotException.get());
    }

    public void testRejectRequest_Sync() throws Exception {
        int count = 10;
        AtomicInteger overflowExceptionCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        ParallelThread parallelThread = new ParallelThread(count, "testRejectRequest");
        parallelThread.execute(() -> {
            try {
                EasyHttp.callSync(
                        Call.builder(Map.class)
                                .withServerAndApi("testServer", "delay_timeout_5000")
                                .addQueryParam("delay", 500)
                                .build()
                );
                successCount.incrementAndGet();
            } catch (EasyResilienceOverflowException e) {
                overflowExceptionCount.incrementAndGet();
            } catch (Exception e) {
                log.error("Unexpected exception: e={}", e.getMessage());
            }
        });
        assertEquals(6, overflowExceptionCount.get());
        assertEquals(4, successCount.get());
    }

    public void testSuccessAsync() throws Exception {
        int count = 1;
        CountDownLatch wait = new CountDownLatch(count);
        AtomicInteger overflowExceptionCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();

        long runningThreadId = Thread.currentThread().getId();
        AtomicLong successThreadId = new AtomicLong();
        log.info("Running in Thread={}", Thread.currentThread().getName());
        callAsync(
                Call.builder(Map.class)
                        .withServerAndApi("testServer", "delay_timeout_5000")
                        .addQueryParam("delay", 500)
                        .build()
        ).subscribe(
                map -> {
                    log.info("Get response in Thread={}", Thread.currentThread().getName());
                    successCount.incrementAndGet();
                    successThreadId.set(Thread.currentThread().getId());
                    wait.countDown();
                },
                throwable -> {
                    if (throwable instanceof EasyResilienceOverflowException) {
                        overflowExceptionCount.incrementAndGet();
                    } else {
                        log.error("Unexpected exception: e={}, cls={}", throwable.getMessage(), throwable.getClass());
                    }
                    wait.countDown();
                })
                .dispose();
        wait.await(10, TimeUnit.SECONDS);
        assertEquals(1, successCount.get());
        assertEquals("Running and success callback thread must be same", runningThreadId, successThreadId.get());
    }

    public void testErrorAsync() throws Exception {
        int count = 1;
        CountDownLatch wait = new CountDownLatch(count);
        AtomicInteger errorCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        long runningThreadId = Thread.currentThread().getId();
        AtomicLong errorThreadId = new AtomicLong();
        log.info("Running in Thread={}", Thread.currentThread().getName());
        callAsync(
                Call.builder(Map.class)
                        .withServerAndApi("testServer", "delay_timeout_1")
                        .addQueryParam("delay", 500)
                        .build()
        ).subscribe(
                map -> {
                    log.error("Unexpected call - did not expected success");
                    successCount.incrementAndGet();
                    wait.countDown();
                },
                throwable -> {
                    log.info("Get error response in Thread={}", Thread.currentThread().getName());
                    errorThreadId.set(Thread.currentThread().getId());
                    errorCount.incrementAndGet();
                    wait.countDown();
                })
                .dispose();
        wait.await(10, TimeUnit.SECONDS);
        assertEquals(0, successCount.get());
        assertEquals(1, errorCount.get());
        assertEquals("Running and error callback thread must be same", runningThreadId, errorThreadId.get());
    }

    public void testRejectRequest_Async() throws Exception {
        int count = 10;
        CountDownLatch wait = new CountDownLatch(count);
        AtomicInteger overflowExceptionCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        ParallelThread parallelThread = new ParallelThread(count, "testRejectRequest");
        parallelThread.execute(() -> {
            callAsync(
                    Call.builder(Map.class)
                            .withServerAndApi("testServer", "delay_timeout_5000")
                            .addQueryParam("delay", 500)
                            .build()
            ).subscribe(
                    map -> {
                        successCount.incrementAndGet();
                        wait.countDown();
                    },
                    throwable -> {
                        if (throwable instanceof EasyResilienceOverflowException) {
                            overflowExceptionCount.incrementAndGet();
                        } else {
                            log.error("Unexpected exception: e={}, cls={}", throwable.getMessage(), throwable.getClass());
                        }
                        wait.countDown();
                    })
                    .dispose();
        });
        wait.await(10, TimeUnit.SECONDS);
        assertEquals(6, overflowExceptionCount.get());
        assertEquals(4, successCount.get());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        localHttpServer.stopServer();
    }
}
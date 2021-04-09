package io.github.devlibx.easy.http.async;

import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.ParallelThread;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.http.BaseTestCase;
import io.github.devlibx.easy.http.config.Config;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyResilienceOverflowException;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.github.devlibx.easy.http.sync.SyncRequestTest.Payload;
import io.github.devlibx.easy.http.util.Call;
import io.github.devlibx.easy.http.util.EasyHttp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.devlibx.easy.http.util.EasyHttp.callAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"ResultOfMethodCallIgnored", "rawtypes"})
@Slf4j
public class AsyncRequestProcessorTest extends BaseTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Thread.sleep(1000);
    }

    @Override
    protected Config getConfig() {
        Config config = super.getConfig();
        config.getApis().forEach((apiName, api) -> api.setAsync(true));
        return config;
    }

    @Test
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
    @Test
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

    @Test
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

    @Test
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
        assertEquals(runningThreadId, successThreadId.get(), "Running and success callback thread must be same");
    }

    @Test
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
        assertEquals(runningThreadId, errorThreadId.get(), "Running and error callback thread must be same");
    }

    @Test
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

    @Test
    public void testAsyncPostRequest() throws Exception {
        AtomicReference<StringObjectMap> data = new AtomicReference<>();
        Payload payload = Payload.createPayload();
        CountDownLatch wait = new CountDownLatch(1);
        EasyHttp.callAsync(
                Call.builder(StringObjectMap.class)
                        .withServerAndApi("testServer", "post_api_with_delay_2000")
                        .addQueryParam("delay", 1)
                        .withBody(payload)
                        .addHeaders("int_header", 67, "string_header", "str_89")
                        .build()
        ).subscribe(stringObjectMap -> {
            data.set(stringObjectMap);
            wait.countDown();
        }, throwable -> {
            wait.countDown();
        });
        wait.await(100, TimeUnit.SECONDS);
        assertNotNull(data.get());
        StringObjectMap response = data.get();
        assertEquals("post", response.get("method"));
        assertFalse(Strings.isNullOrEmpty(response.getString("request_body")));
        assertFalse(Strings.isNullOrEmpty(response.getString("headers")));

        Payload responseBody = JsonUtils.readObject(response.getString("request_body"), Payload.class);
        assertNotNull(responseBody);
        assertEquals(payload, responseBody);

        StringObjectMap headers = JsonUtils.convertAsStringObjectMap(response.getString("headers"));
        assertEquals("67", headers.getList("Int_header", String.class).get(0));
        assertEquals("str_89", headers.getList("String_header", String.class).get(0));
    }

    @Test
    public void testAsyncPutRequest() throws InterruptedException {
        AtomicReference<StringObjectMap> data = new AtomicReference<>();
        Payload payload = Payload.createPayload();
        CountDownLatch wait = new CountDownLatch(1);
        EasyHttp.callAsync(
                Call.builder(StringObjectMap.class)
                        .withServerAndApi("testServer", "put_api_with_delay_2000")
                        .addQueryParam("delay", 1)
                        .withBody(payload)
                        .addHeaders("int_header", 67, "string_header", "str_89")
                        .build()
        ).subscribe(stringObjectMap -> {
            data.set(stringObjectMap);
            wait.countDown();
        }, throwable -> {
            log.debug("Unexpected exception - ", throwable);
            wait.countDown();
        });
        wait.await(100, TimeUnit.SECONDS);
        assertNotNull(data.get());
        StringObjectMap response = data.get();
        assertEquals("put", response.get("method"));
        assertFalse(Strings.isNullOrEmpty(response.getString("request_body")));
        assertFalse(Strings.isNullOrEmpty(response.getString("headers")));

        Payload responseBody = JsonUtils.readObject(response.getString("request_body"), Payload.class);
        assertNotNull(responseBody);
        assertEquals(payload, responseBody);

        StringObjectMap headers = JsonUtils.convertAsStringObjectMap(response.getString("headers"));
        assertEquals("67", headers.getList("Int_header", String.class).get(0));
        assertEquals("str_89", headers.getList("String_header", String.class).get(0));

    }
}
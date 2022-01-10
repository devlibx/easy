package io.github.devlibx.easy.http.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.ParallelThread;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.http.BaseTestCase;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyResilienceOverflowException;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.github.devlibx.easy.http.util.Call;
import io.github.devlibx.easy.http.util.EasyHttp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("rawtypes")
@Slf4j
public class SyncRequestTest extends BaseTestCase {

    /**
     * Test a simple http call (with success)
     */
    @Test
    public void testSimpleHttpRequestWithHeadersInConfig() {
        StringObjectMap resultSync = EasyHttp.callSync(
                Call.builder(StringObjectMap.class)
                        .withServerAndApi("testServer", "getPostsWithHeaders")
                        .addQueryParam("delay", 1)
                        .build()
        );
        assertEquals("1", resultSync.get("delay"));
        assertEquals("some data", resultSync.get("data"));
        String headersString = resultSync.getString("headers", "{}");
        StringObjectMap headers = JsonUtils.convertAsStringObjectMap(headersString);
        assertEquals("value", headers.getList("Key", String.class).get(0));
        assertEquals("10", headers.getList("Key1", String.class).get(0));
    }

    /**
     * Test a simple http call (with success)
     */
    @Test
    public void testSimpleHttpRequest() {
        Map resultSync = EasyHttp.callSync(
                Call.builder(Map.class)
                        .withServerAndApi("testServer", "delay_timeout_5000")
                        .addQueryParam("delay", 1000)
                        .build()
        );
        assertEquals("1000", resultSync.get("delay"));
        assertEquals("some data", resultSync.get("data"));
    }

    /**
     * Test a simple http call (with success)
     */
    @Test
    public void testSimpleHttpRequest_WithResponseBuilder() {
        StringObjectMap resultSync = EasyHttp.callSync(
                Call.builder(StringObjectMap.class)
                        .withServerAndApi("testServer", "delay_timeout_5000")
                        .withResponseBuilder(bytes -> {
                            StringObjectMap toReturn = JsonUtils.convertAsStringObjectMap(bytes);
                            toReturn.put("processed", true);
                            return toReturn;
                        })
                        .addQueryParam("delay", 1000)
                        .build()
        );
        assertEquals(1000, resultSync.getInt("delay").intValue());
        assertEquals("some data", resultSync.getString("data"));
        assertEquals(Boolean.TRUE, resultSync.getBoolean("processed"));
    }


    /**
     * Test a simple http call where we make too many calls to simulate requests rejected
     */
    @Test
    public void testRequestExpectRejected() throws Exception {
        AtomicInteger overflowCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        ParallelThread parallelThread = new ParallelThread(10, "testRequestExpectRejected");
        parallelThread.execute(() -> {
            try {
                EasyHttp.callSync(
                        Call.builder(Map.class)
                                .withServerAndApi("testServer", "delay_timeout_1000")
                                .addQueryParam("delay", 100)
                                .build()
                );
                successCount.incrementAndGet();
            } catch (EasyResilienceOverflowException e) {
                overflowCount.incrementAndGet();
                log.info("Test is expecting this exception - " + e);
            } catch (Exception e) {
                log.error("Test expecting is not expected - " + e);
                // fail("We should not get a exception - only OverflowException is expected");
            }
        });
        assertEquals(6, overflowCount.get());
        assertEquals(4, successCount.get());
    }

    /**
     * Test a simple http call where we make too many calls to simulate requests rejected
     */
    @Test
    public void testRequestTimeout() throws Exception {
        CountDownLatch wait = new CountDownLatch(1);
        AtomicBoolean gotException = new AtomicBoolean(false);
        try {
            EasyHttp.callSync(
                    Call.builder(Map.class)
                            .withServerAndApi("testServer", "delay_timeout_10")
                            .addQueryParam("delay", 100)
                            .build()
            );
            System.out.println("Got it working");
            wait.countDown();
        } catch (EasyResilienceRequestTimeoutException | EasyRequestTimeOutException e) {
            log.error("Not expected - error1", e);
            gotException.set(true);
            wait.countDown();
        } catch (Throwable e) {
            log.error("Not expected - error", e);
            wait.countDown();
        }
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotException.get());
    }

    @Test
    public void testSyncPostRequest() {
        Payload payload = Payload.createPayload();
        StringObjectMap response = EasyHttp.callSync(
                Call.builder(StringObjectMap.class)
                        .withServerAndApi("testServer", "post_api_with_delay_2000")
                        .addQueryParam("delay", 1)
                        .withBody(payload)
                        .addHeaders("int_header", 67, "string_header", "str_89")
                        .build()
        );
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
    public void testSyncPutRequest() {
        Payload payload = Payload.createPayload();
        StringObjectMap response = EasyHttp.callSync(
                Call.builder(StringObjectMap.class)
                        .withServerAndApi("testServer", "put_api_with_delay_2000")
                        .addQueryParam("delay", 1)
                        .withBody(payload)
                        .addHeaders("int_header", 67, "string_header", "str_89")
                        .build()
        );
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private int intValue;
        private String stringValue;
        private StringObjectMap mapValue;

        public static Payload createPayload() {
            StringObjectMap m = new StringObjectMap();
            m.put("key1", "value1");
            Payload payload = new Payload();
            payload.intValue = 11;
            payload.stringValue = UUID.randomUUID().toString();
            payload.mapValue = new StringObjectMap();
            payload.mapValue.put("key_int", 34, "key_map", m);
            return payload;
        }
    }
}

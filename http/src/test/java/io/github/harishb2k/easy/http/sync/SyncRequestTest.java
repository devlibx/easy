package io.github.harishb2k.easy.http.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;
import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.ParallelThread;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.gitbub.harishb2k.easy.helper.map.StringObjectMap;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceOverflowException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.github.harishb2k.easy.http.util.Call;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("rawtypes")
@Slf4j
public class SyncRequestTest extends TestCase {
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
        EasyHttp.setup(config);

        Thread.sleep(5000);
    }

    /**
     * Test a simple http call (with success)
     */
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
            System.out.println("--> " + e);
            wait.countDown();
        }
        wait.await(10, TimeUnit.SECONDS);
        assertTrue(gotException.get());
    }

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

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        localHttpServer.stopServer();
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

package io.github.devlibx.easy.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.file.FileHelper;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Config;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions;
import io.github.devlibx.easy.http.module.EasyHttpModule;
import io.github.devlibx.easy.http.sync.SyncRequestProcessor;
import io.github.devlibx.easy.http.util.Call;
import io.github.devlibx.easy.http.util.EasyHttp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ch.qos.logback.classic.Level.TRACE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class DemoApplication {

    @BeforeEach
    protected void setUp() throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(SyncRequestProcessor.class).setLevel(TRACE);
        LoggingHelper.getLogger(FileHelper.class).setLevel(TRACE);
        LoggingHelper.getLogger(IMetrics.ConsoleOutputMetrics.class).setLevel(TRACE);

        // Setup injector (Onetime MUST setup before we call EasyHttp.setup())
        // Or if you do not any console logs
        // bind(IMetrics.class).to(IMetrics.NoOpMetrics.class).in(Scopes.SINGLETON);
        // Or you can use Prometheus Module to get Prometheus metrics
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.ConsoleOutputMetrics.class).in(Scopes.SINGLETON);

                // Or if you do not any console logs
                // bind(IMetrics.class).to(IMetrics.NoOpMetrics.class).in(Scopes.SINGLETON);

                // Or you can use Prometheus Module to get Prometheus metrics
            }
        }, new EasyHttpModule());
        ApplicationContext.setInjector(injector);

        // Read config and setup EasyHttp
        Config config = YamlUtils.readYamlCamelCase("demo_app_config.yaml", Config.class);
        EasyHttp.setup(config);
    }

    @Test
    public void testSyncApiCall() {

        // Example 1 - Make a call and get response in a Map
        Map result = EasyHttp.callSync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPosts")
                        .addPathParam("id", 1)
                        .build()
        );
        log.info("Print Result as Json String = " + JsonUtils.asJson(result));
        // Result = {"userId":1,"id":1,"title":"some text ..."}

        // Example 2 - Make a call and get response in a Pojo
        ResponsePojo resultWithPojo = EasyHttp.callSync(
                Call.builder(ResponsePojo.class)
                        .withServerAndApi("jsonplaceholder", "getPosts")
                        .addPathParam("id", 1)
                        .build()
        );
        String jsonString = JsonUtils.asJson(resultWithPojo);
        log.info("Print Result as Json String = " + jsonString);
        // Print Result as Json String = {"userId":1,"id":1,"title":"sunt aut facere repellat provident occaecati excepturi optio reprehenderit","completed":false}
    }

    @Test
    public void testSyncApiCallWithError() {
        // Example 1 - Make a call and get process error
        try {
            ResponsePojo resultWithPojoError = EasyHttp.callSync(
                    Call.builder(ResponsePojo.class)
                            .withServerAndApi("jsonplaceholder", "getPosts")
                            .addPathParam("id_make_it_fail", 1)
                            .build()
            );

            // You can catch
            // EasyHttpExceptions.Easy4xxException e1;
            // EasyHttpExceptions.EasyUnauthorizedRequestException e;
            // EasyHttpExceptions.EasyRequestTimeOutException e;
        } catch (EasyHttpExceptions.Easy5xxException e) {
            // You can cache specific errors
            log.error("Api failed (5xx error): status=" + e.getStatusCode() + " byteBody=" + e.getBody());
        } catch (EasyHttpExceptions.EasyHttpRequestException e) {
            log.error("Api failed: status=" + e.getStatusCode() + " byteBody=" + e.getBody());
        }
    }

    @Test
    public void testAsyncApiCall() throws Exception {
        CountDownLatch waitForComplete = new CountDownLatch(1);
        EasyHttp.callAsync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPostsAsync")
                        .addPathParam("id", 1)
                        .build()
        ).subscribe(
                result -> {
                    log.info("Print Result as Json String = " + JsonUtils.asJson(result));
                    // Result = {"userId":1,"id":1,"title":"some text ..."}
                    waitForComplete.countDown();
                },
                throwable -> {
                    waitForComplete.countDown();
                });
        waitForComplete.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testConfigProcessor() {
        // Read config and setup EasyHttp
        Config config = YamlUtils.readYamlCamelCase("app_config_to_test_pre_processor.yaml", Config.class);

        EasyHttp.setup(config);

        Api api = config.getApis().get("getPostsAsync_Test");
        assertEquals(500, api.getTimeout());
        assertEquals(150, api.getConcurrency());

        api = config.getApis().get("getPostsAsync_Test_1");
        assertEquals(500, api.getTimeout());
        assertEquals(150, api.getConcurrency());
        assertEquals(123, api.getQueueSize());
    }

    @Data
    private static class ResponsePojo {
        @JsonProperty("userId")
        private Integer userId;
        @JsonProperty("id")
        private Integer id;
        @JsonProperty("title")
        private String title;
        @JsonProperty("completed")
        private boolean completed;
    }
}

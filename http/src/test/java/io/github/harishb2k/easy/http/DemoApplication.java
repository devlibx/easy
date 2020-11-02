package io.github.harishb2k.easy.http;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.file.FileHelper;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.module.EasyHttpModule;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;
import io.github.harishb2k.easy.http.util.Call;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ch.qos.logback.classic.Level.TRACE;

@Slf4j
public class DemoApplication extends TestCase {
    private Injector injector;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(SyncRequestProcessor.class).setLevel(TRACE);
        LoggingHelper.getLogger(FileHelper.class).setLevel(TRACE);
        LoggingHelper.getLogger(IMetrics.ConsoleOutputMetrics.class).setLevel(TRACE);

        // Setup injector (Onetime MUST setup before we call EasyHttp.setup())
        injector = Guice.createInjector(new AbstractModule() {
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

    public void testSyncApiCall() {
        Map result = EasyHttp.callSync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPosts")
                        .addPathParam("id", 1)
                        .build()
        );

        log.info("Print Result as Json String = " + JsonUtils.asJson(result));
        // Result = {"userId":1,"id":1,"title":"some text ..."}
    }

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
}

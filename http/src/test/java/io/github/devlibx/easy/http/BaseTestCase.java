package io.github.devlibx.easy.http;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LocalHttpServer;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.file.FileHelper;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.http.async.AsyncRequestProcessor;
import io.github.devlibx.easy.http.config.Config;
import io.github.devlibx.easy.http.module.EasyHttpModule;
import io.github.devlibx.easy.http.util.EasyHttp;
import org.apache.http.HttpClientConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static ch.qos.logback.classic.Level.OFF;

public abstract class BaseTestCase {
    protected LocalHttpServer localHttpServer;
    protected Injector injector;

    @BeforeEach
    public void setUp() throws Exception {
        // Setup logging
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(LocalHttpServer.class).setLevel(OFF);
        LoggingHelper.getLogger(FileHelper.class).setLevel(OFF);
        LoggingHelper.getLogger(HttpClientConnection.class).setLevel(OFF);
        LoggingHelper.getLogger(AsyncRequestProcessor.class).setLevel(OFF);

        // Start server
        localHttpServer = new LocalHttpServer();
        localHttpServer.startServerInThread();

        // Setup injector
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.ConsoleOutputMetrics.class).in(Scopes.SINGLETON);
            }
        }, new EasyHttpModule());
        ApplicationContext.setInjector(injector);

        // Read config and setup EasyHttp
        Config config = getConfig();
        EasyHttp.setup(config);
    }

    protected Config getConfig() {
        Config config = YamlUtils.readYamlCamelCase("sync_processor_config.yaml", Config.class);
        config.getServers().get("testServer").setPort(localHttpServer.port);
        return config;
    }

    @AfterEach
    public void tearDown() throws Exception {
        localHttpServer.stopServer();
        EasyHttp.shutdown();
    }
}

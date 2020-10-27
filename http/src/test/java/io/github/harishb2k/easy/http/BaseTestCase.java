package io.github.harishb2k.easy.http;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.file.FileHelper;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.async.AsyncRequestProcessor;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.module.EasyHttpModule;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import org.apache.http.HttpClientConnection;
import org.apache.log4j.Logger;

import static org.apache.log4j.Level.OFF;

public abstract class BaseTestCase extends TestCase {
    protected LocalHttpServer localHttpServer;
    protected Injector injector;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Setup logging
        LoggingHelper.setupLogging();
        Logger.getLogger(LocalHttpServer.class).setLevel(OFF);
        Logger.getLogger(FileHelper.class).setLevel(OFF);
        Logger.getLogger(HttpClientConnection.class).setLevel(OFF);
        Logger.getLogger(AsyncRequestProcessor.class).setLevel(OFF);

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

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        localHttpServer.stopServer();
        EasyHttp.shutdown();
    }
}

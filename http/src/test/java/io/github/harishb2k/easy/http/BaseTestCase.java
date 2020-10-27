package io.github.harishb2k.easy.http;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.module.EasyHttpModule;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import static org.apache.log4j.Level.TRACE;

public abstract class BaseTestCase extends TestCase {
    protected LocalHttpServer localHttpServer;
    protected Injector injector;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Setup logging
        LoggingHelper.setupLogging();
        Logger.getLogger(LocalHttpServer.class).setLevel(TRACE);

        // Start server
        localHttpServer = new LocalHttpServer();
        localHttpServer.startServerInThread();

        // Setup injector
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
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
    }
}

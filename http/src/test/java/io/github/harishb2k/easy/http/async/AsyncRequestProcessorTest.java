package io.github.harishb2k.easy.http.async;

import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.github.harishb2k.easy.http.util.Call;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncRequestProcessorTest extends TestCase {
    private LocalHttpServer localHttpServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Setup logging
        setupLogging();

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

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        localHttpServer.stopServer();
    }

    private void setupLogging() {
        ConsoleAppender console = new ConsoleAppender();
        // String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        String PATTERN = "%m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(org.apache.log4j.Level.DEBUG);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        Logger.getLogger("io.github.harishb2k.easy.http.sync").setLevel(Level.OFF);
        Logger.getLogger(LocalHttpServer.class).setLevel(Level.DEBUG);
        Logger.getLogger("reactor.netty.http.client").setLevel(Level.OFF);
    }
}
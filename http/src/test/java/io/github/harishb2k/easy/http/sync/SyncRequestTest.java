package io.github.harishb2k.easy.http.sync;

import io.gitbub.harishb2k.easy.helper.LocalHttpServer;
import io.gitbub.harishb2k.easy.helper.ParallelThread;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.util.EasyHttp;
import io.github.harishb2k.easy.resilience.exception.OverflowException;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.harishb2k.easy.http.helper.CommonHttpHelper.multivaluedMap;

@SuppressWarnings("rawtypes")
public class SyncRequestTest extends TestCase {
    private LocalHttpServer localHttpServer;


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
    }

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
        EasyHttp.setup(config);
    }

    /**
     * Test a simple http call (with success)
     */
    public void testSimpleHttpRequest() {
        Map resultSync = EasyHttp.callSync(
                "testServer",
                "delay_timeout_100",
                null,
                multivaluedMap("delay", 1),
                null,
                null,
                Map.class
        );
        assertEquals("1", resultSync.get("delay"));
        assertEquals("some data", resultSync.get("data"));
    }

    /**
     * Test a simple http call where we make too many calls to simulate requets rejected
     */
    public void testRequestExpectRejected() throws Exception {
        AtomicInteger overflowCount = new AtomicInteger();
        ParallelThread parallelThread = new ParallelThread(10, "testRequestExpectRejected");
        parallelThread.execute(() -> {
            try {
                EasyHttp.callSync(
                        "testServer",
                        "delay_timeout_1000",
                        null,
                        multivaluedMap("delay", 1),
                        null,
                        null,
                        Map.class
                );
            } catch (OverflowException e) {
                overflowCount.incrementAndGet();
                System.out.println("Test is expecting this exception - " + e);
            } catch (Exception e) {
                fail("We should not get a exception - only OverflowException is expected");
            }
        });
        assertEquals(6, overflowCount.get());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        localHttpServer.stopServer();
    }
}

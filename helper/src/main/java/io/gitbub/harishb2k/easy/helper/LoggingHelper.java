package io.gitbub.harishb2k.easy.helper;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.checkerframework.checker.units.qual.A;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.log4j.Level.DEBUG;
import static org.apache.log4j.Level.INFO;
import static org.apache.log4j.Level.OFF;
import static org.apache.log4j.Level.TRACE;

public class LoggingHelper {
    private static final AtomicBoolean initDone = new AtomicBoolean(false);

    /**
     * Helper to setup logger
     */
    public static void setupLogging() {
        try {
            setupLoggingApacheLog4j();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupLoggingApacheLog4j() {
        synchronized (initDone) {
            if (initDone.get()) return;
            initDone.set(true);

            ConsoleAppender console = new ConsoleAppender();
            // String PATTERN = "%d [%p|%c|%C{1}] %m%n";
            String PATTERN = "%d [%C{1}] %m%n";
            console.setLayout(new PatternLayout(PATTERN));
            console.setThreshold(DEBUG);
            console.activateOptions();
            // Logger.getRootLogger().addAppender(console);
            Logger.getRootLogger().setLevel(INFO);
            Logger.getLogger("io.github.harishb2k.easy.http.sync").setLevel(OFF);
            Logger.getLogger(LocalHttpServer.class).setLevel(INFO);
        }
    }
}

package io.gitbub.harishb2k.easy.helper;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import static org.apache.log4j.Level.DEBUG;
import static org.apache.log4j.Level.INFO;
import static org.apache.log4j.Level.OFF;
import static org.apache.log4j.Level.TRACE;

public class LoggingHelper {

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
        ConsoleAppender console = new ConsoleAppender();
        // String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        String PATTERN = "%d [%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(DEBUG);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        Logger.getRootLogger().setLevel(INFO);
        Logger.getLogger("io.github.harishb2k.easy.http.sync").setLevel(OFF);
        Logger.getLogger(LocalHttpServer.class).setLevel(INFO);
    }
}

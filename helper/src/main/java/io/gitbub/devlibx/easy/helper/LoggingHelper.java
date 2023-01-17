package io.gitbub.devlibx.easy.helper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoggingHelper {
    private static final AtomicBoolean initDone = new AtomicBoolean(false);

    /**
     * Helper to setup logger
     */
    public static void setupLogging() {
        try {
            setupLoggingLogbackClassic();
        } catch (Exception e) {
            try {
                setupLoggingApacheLog4j();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private static void setupLoggingApacheLog4j() {
        synchronized (initDone) {
            if (initDone.get()) return;
            initDone.set(true);

           /* ConsoleAppender console = new ConsoleAppender();
            // String PATTERN = "%d [%p|%c|%C{1}] %m%n";
            String PATTERN = "%d [%C{1}] %m%n";
            console.setLayout(new PatternLayout(PATTERN));
            console.setThreshold(DEBUG);
            console.activateOptions();
            // Logger.getRootLogger().addAppender(console);
            Logger.getRootLogger().setLevel(INFO);
            Logger.getLogger("io.github.devlibx.easy.http.sync").setLevel(OFF);
            Logger.getLogger(LocalHttpServer.class).setLevel(INFO);*/
        }
    }

    private static void setupLoggingLogbackClassic() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
        loggerContext.getLogger("com.github.dockerjava.core.command").setLevel(Level.OFF);
    }

    public static Logger getLogger(Class<?> clazz) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(name);
    }
}

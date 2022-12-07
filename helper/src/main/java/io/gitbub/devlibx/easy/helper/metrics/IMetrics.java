package io.gitbub.devlibx.easy.helper.metrics;

import com.google.inject.ImplementedBy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@ImplementedBy(IMetrics.NoOpMetrics.class)
public interface IMetrics {

    /**
     * Get the underlying registry for this metrics
     */
    <T> T getRegistry(Class<T> cls);

    /**
     * Count this metrics
     */
    void inc(String name, String... labels);

    /**
     * Count this metrics
     */
    default void inc(String name, long count, String... labels) {
        inc(name, 1, labels);
    }

    /**
     * Time this callable
     */
    <T> T time(String name, Callable<T> callable, String... labels);

    /**
     * Add time taken to given metrics
     */
    default void observe(String name, double amt) {
    }


    /**
     * Add time taken to given metrics
     */
    default void observe(String name, double amt, String... labelNames) {
    }

    /**
     * Register a counter
     */
    void registerCounter(String name, String help, String... labelNames);

    /**
     * Register a timer
     */
    void registerTimer(String name, String help, String... labelNames);

    // No-Op metrics - ignore all calls
    class NoOpMetrics implements IMetrics {

        @Override
        public <T> T getRegistry(Class<T> cls) {
            return null;
        }

        @Override
        public void inc(String name, String... labels) {
        }

        @Override
        public <T> T time(String name, Callable<T> callable, String... labels) {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void registerCounter(String name, String help, String... labelNames) {
        }

        @Override
        public void registerTimer(String name, String help, String... labelNames) {
        }
    }

    // No-Op metrics - ignore all calls
    @Slf4j
    class ConsoleOutputMetrics implements IMetrics {

        @Override
        public <T> T getRegistry(Class<T> cls) {
            return null;
        }

        @Override
        public void inc(String name, String... labels) {
            log.debug("increment metrics={}", name);
        }

        @Override
        public <T> T time(String name, Callable<T> callable, String... labels) {
            long start = System.currentTimeMillis();
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                log.debug("time taken by metrics={} is {}", name, (System.currentTimeMillis() - start));
            }
        }

        @Override
        public void observe(String name, double amt) {
            log.debug("time taken by metrics={} is {}", name, amt);
        }

        @Override
        public void registerCounter(String name, String help, String... labelNames) {
        }

        @Override
        public void registerTimer(String name, String help, String... labelNames) {
        }
    }

    interface IMetricsLogger {
        void printf(String format, Object... args);
    }

    class NoOpMetricsLogger implements IMetricsLogger {
        @Override
        public void printf(String format, Object... args) {
        }
    }

    class MetricsLogger {
        public void printf(String format, Object... args) {
            System.out.printf(format, args);
        }
    }

    class InvalidRegistryTypeFoundException extends RuntimeException {
        public InvalidRegistryTypeFoundException(String message) {
            super(message);
        }
    }
}

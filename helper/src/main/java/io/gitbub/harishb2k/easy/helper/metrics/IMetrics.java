package io.gitbub.harishb2k.easy.helper.metrics;

import java.util.concurrent.Callable;

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
     * Time this callable
     */
    <T> T time(String name, Callable<T> callable, String... labels);

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

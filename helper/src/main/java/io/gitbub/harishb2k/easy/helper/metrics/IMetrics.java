package io.gitbub.harishb2k.easy.helper.metrics;

import java.util.concurrent.Callable;

public interface IMetrics {

    <T> T time(String name, Callable<T> callable, String... labels);

    class NoOpMetrics implements IMetrics {
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
    }
}

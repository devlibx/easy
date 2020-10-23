package io.github.harishb2k.easy.resilience;

import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;

public interface IResilienceManager {

    /**
     * @return get existing or a new processor to handle this request
     */
    IResilienceProcessor getOrCreate(ResilienceCallConfig config);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    class ResilienceCallConfig {
        private String id;
        private int concurrency = 10;
        private int timeout = 1000;
        private int queueSize = 100;

        public static ResilienceCallConfigBuilder withDefaults() {
            return ResilienceCallConfig.builder()
                    .queueSize(100)
                    .timeout(1000)
                    .concurrency(10);
        }
    }

    interface IResilienceProcessor {
        /**
         * Setup processor
         */
        void initialized(ResilienceCallConfig config);

        /**
         * Execute a request
         */
        <T> T execute(String id, Callable<T> callable, Class<T> cls) throws ResilienceException;

        <T> Observable<T> executeAsObservable(String id, Callable<T> callable, Class<T> cls);
    }

    class ResilienceException extends RuntimeException {
        public ResilienceException(String message, Throwable e) {
            super(message, e);
        }
    }

    class OverflowException extends ResilienceException {
        public OverflowException(String message, Throwable e) {
            super("Request overflow - " + message, e);
        }
    }

    class CircuitOpenException extends ResilienceException {
        public CircuitOpenException(String message, Throwable e) {
            super("Circuit is open - " + message, e);
        }
    }

    class RequestTimeoutException extends ResilienceException {
        public RequestTimeoutException(String message, Throwable e) {
            super("Request timeout - " + message, e);
        }
    }

    class UnknownException extends ResilienceException {
        public UnknownException(String message, Throwable e) {
            super("Unknown error - " + message, e);
        }
    }
}

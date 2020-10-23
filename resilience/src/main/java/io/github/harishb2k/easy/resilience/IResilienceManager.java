package io.github.harishb2k.easy.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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

        <T> Observable<T> executeAsObservable(String id, Observable<T> observable, Class<T> cls);
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

    default RuntimeException processException(Exception e) {
        if (e instanceof UnknownException) {
            return (RuntimeException) e.getCause();
        } else if (e.getCause() instanceof ExecutionException) {
            return processException((Exception) e.getCause());
        } else {
            return new RuntimeException(e);
        }
    }

    default RuntimeException processException(ExecutionException e) {
        if (e.getCause() instanceof BulkheadFullException) {
            BulkheadFullException exception = (BulkheadFullException) e.getCause();
            return new OverflowException(exception.getMessage(), e);
        } else if (e.getCause() instanceof CallNotPermittedException) {
            CallNotPermittedException exception = (CallNotPermittedException) e.getCause();
            return new CircuitOpenException(exception.getMessage(), e);
        } else if (e.getCause() instanceof TimeoutException) {
            return new RequestTimeoutException(e.getMessage(), e);
        } else {
            return new UnknownException(e.getMessage(), e.getCause());
        }
    }
}

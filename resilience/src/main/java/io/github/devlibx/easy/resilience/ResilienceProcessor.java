package io.github.devlibx.easy.resilience;

import io.github.devlibx.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.devlibx.easy.resilience.exception.ExceptionUtil;
import io.github.devlibx.easy.resilience.exception.ResilienceException;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

import static io.github.devlibx.easy.resilience.exception.ExceptionUtil.unwrapResilience4jException;
import static io.github.devlibx.easy.resilience.exception.ExceptionUtil.unwrapResilience4jExecutionException;

public class ResilienceProcessor implements IResilienceProcessor {
    @Getter
    private CircuitBreaker circuitBreaker;
    private ThreadPoolBulkhead threadPoolBulkhead;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService retryScheduler;
    private TimeLimiter timeLimiter;
    private SemaphoreBulkhead semaphoreBulkhead;
    private ResilienceCallConfig config;
    private Retry retry;

    @Override
    public void initialized(ResilienceCallConfig config) {
        this.config = config;

        // Setup a circuit breaker with default settings
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofMillis(config.getWaitDurationInOpenState() <= 0 ? 10000 : config.getWaitDurationInOpenState()))
                .ignoreException(throwable -> {
                    if (throwable instanceof IgnorableException) {
                        IgnorableException ie = (IgnorableException) throwable;
                        return ie.canIgnoreException();
                    }
                    return false;
                })
                .build();
        circuitBreaker = CircuitBreaker.of(config.getId(), circuitBreakerConfig);

        // Create bulk head
        if (config.isUseSemaphore()) {
            BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(config.getConcurrency() + config.getQueueSize())
                    .build();
            semaphoreBulkhead = new SemaphoreBulkhead(config.getId(), bulkheadConfig);
        } else {

            // Create thread bulk head
            ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
                    .coreThreadPoolSize(config.getConcurrency())
                    .maxThreadPoolSize(config.getConcurrency())
                    .queueCapacity(config.getQueueSize())
                    .build();
            threadPoolBulkhead = ThreadPoolBulkhead.of(config.getId(), threadPoolBulkheadConfig);

            // A scheduler and time limiter to handle timeouts
            scheduler = Executors.newScheduledThreadPool(config.getConcurrency());
            timeLimiter = TimeLimiter.of(Duration.ofMillis(config.getTimeout()));


            // Build retry
            if (config.getRetryCount() > 0) {
                if (config.getRetryWaitDurationMs() <= 0) {
                    config.setRetryWaitDurationMs(100);
                }
                if (config.getRetryRequestThreadPoolCount() <= 0) {
                    config.setRetryRequestThreadPoolCount(3);
                }
                RetryConfig retryConfig = RetryConfig.custom()
                        .maxAttempts(config.getRetryCount())
                        .waitDuration(Duration.ofMillis(config.getRetryWaitDurationMs()))
                        .build();
                retryScheduler = Executors.newScheduledThreadPool(config.getRetryRequestThreadPoolCount());
                retry = Retry.of(config.getId(), retryConfig);
            }
        }
    }

    @Override
    public <T> T execute(String id, Callable<T> callable, Class<T> cls) throws ResilienceException {
        try {
            CompletableFuture<T> future;
            if (retry != null) {
                future = Decorators
                        .ofCallable(callable)
                        .withThreadPoolBulkhead(threadPoolBulkhead)
                        .withTimeLimiter(timeLimiter, scheduler)
                        .withCircuitBreaker(circuitBreaker)
                        .withRetry(retry, retryScheduler)
                        .get()
                        .toCompletableFuture();
            } else {
                future = Decorators
                        .ofCallable(callable)
                        .withThreadPoolBulkhead(threadPoolBulkhead)
                        .withTimeLimiter(timeLimiter, scheduler)
                        .withCircuitBreaker(circuitBreaker)
                        .get()
                        .toCompletableFuture();
            }
            return future.get();
        } catch (ExecutionException e) {
            throw unwrapResilience4jExecutionException(e);
        } catch (Exception e) {
            throw unwrapResilience4jException(e);
        }
    }

    @Override
    public <T> Observable<T> executeObservable(String id, Callable<T> callable, Class<T> cls) {
        return Observable.create(observableEmitter -> {
            try {
                T result = execute(id, callable, cls);
                observableEmitter.onNext(result);
                observableEmitter.onComplete();
            } catch (ResilienceException e) {
                observableEmitter.onError(e);
            } catch (Exception e) {
                // We should never get here. The helper "execute" method never throws exception
                // (it wraps all to ResilienceException)
                observableEmitter.onError(ExceptionUtil.unwrapResilience4jException(e));
            }
        });
    }

    @Override
    public <T> Observable<T> executeObservable(String id, Observable<T> observable, Class<T> cls) {
        return Observable.create(observableEmitter -> {

            if (config.isUseSemaphore()) {
                try {
                    T result = Decorators.ofSupplier(observable::blockingFirst)
                            .withCircuitBreaker(circuitBreaker)
                            .withBulkhead(semaphoreBulkhead)
                            .decorate()
                            .get();
                    whenComplete(observableEmitter).accept(result, null);
                } catch (Exception e) {
                    whenComplete(observableEmitter).accept(null, e);
                }
            } else {
                if (retry != null) {
                    Decorators.ofSupplier(observable::blockingFirst)
                            .withCircuitBreaker(circuitBreaker)
                            .withThreadPoolBulkhead(threadPoolBulkhead)
                            .withTimeLimiter(timeLimiter, scheduler)
                            .withRetry(retry, retryScheduler)
                            .decorate()
                            .get()
                            .whenCompleteAsync(whenComplete(observableEmitter));
                } else {
                    Decorators.ofSupplier(observable::blockingFirst)
                            .withCircuitBreaker(circuitBreaker)
                            .withThreadPoolBulkhead(threadPoolBulkhead)
                            .withTimeLimiter(timeLimiter, scheduler)
                            .decorate()
                            .get()
                            .whenCompleteAsync(whenComplete(observableEmitter));
                }
            }
        });
    }

    private static <T> BiConsumer<T, Throwable> whenComplete(ObservableEmitter<T> observableEmitter) {
        return (t, throwable) -> {
            if (throwable instanceof CompletionException) {
                Exception e = ExceptionUtil.unwrapResilience4jException(throwable.getCause());
                observableEmitter.onError(e);
            } else if (throwable != null) {
                Exception e = ExceptionUtil.unwrapResilience4jException(throwable);
                observableEmitter.onError(e);
            } else {
                observableEmitter.onNext(t);
                observableEmitter.onComplete();
            }
        };
    }
}
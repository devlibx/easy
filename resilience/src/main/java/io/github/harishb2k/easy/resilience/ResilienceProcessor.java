package io.github.harishb2k.easy.resilience;

import io.github.harishb2k.easy.resilience.IResilienceManager.CircuitOpenException;
import io.github.harishb2k.easy.resilience.IResilienceManager.IResilienceProcessor;
import io.github.harishb2k.easy.resilience.IResilienceManager.OverflowException;
import io.github.harishb2k.easy.resilience.IResilienceManager.RequestTimeoutException;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceException;
import io.github.harishb2k.easy.resilience.IResilienceManager.UnknownException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.reactivex.Observable;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

public class ResilienceProcessor implements IResilienceProcessor {
    private CircuitBreaker circuitBreaker;
    private ThreadPoolBulkhead threadPoolBulkhead;
    private ScheduledExecutorService scheduler;
    private TimeLimiter timeLimiter;

    @Override
    public void initialized(ResilienceCallConfig config) {

        circuitBreaker = CircuitBreaker.ofDefaults(config.getId());

        ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(config.getConcurrency())
                .maxThreadPoolSize(config.getConcurrency())
                .queueCapacity(config.getQueueSize())
                .build();
        threadPoolBulkhead = ThreadPoolBulkhead.of(config.getId(), threadPoolBulkheadConfig);

        scheduler = Executors.newScheduledThreadPool(config.getConcurrency());
        timeLimiter = TimeLimiter.of(Duration.ofMillis(config.getTimeout()));
    }

    @Override
    public <T> T execute(String id, Callable<T> callable, Class<T> cls) throws ResilienceException {
        try {
            CompletableFuture<T> future = Decorators.ofCallable(callable)
                    .withThreadPoolBulkhead(threadPoolBulkhead)
                    .withTimeLimiter(timeLimiter, scheduler)
                    .withCircuitBreaker(circuitBreaker)
                    .get()
                    .toCompletableFuture();
            return future.get();
        } catch (ExecutionException e) {
            throw processException(e);
        } catch (Exception e) {
            throw new UnknownException(e.getMessage(), e);
        }
    }

    @Override
    public <T> Observable<T> executeAsObservable(String id, Callable<T> callable, Class<T> cls) {
        return Observable.create(observableEmitter -> {
            try {
                T result = execute(id, callable, cls);
                observableEmitter.onNext(result);
                observableEmitter.onComplete();
            } catch (Exception e) {
                if (e.getCause() instanceof ExecutionException) {
                    observableEmitter.onError(processException((ExecutionException) e.getCause()));
                } else {
                    observableEmitter.onError(e);
                }
            }
        });
    }

    public <T> Observable<T> executeAsObservable(String id, Observable<T> observable, Class<T> cls) {
        return Observable.create(observableEmitter -> {
            try {
                CompletableFuture<T> future = Decorators.ofSupplier(observable::blockingFirst)
                        .withThreadPoolBulkhead(threadPoolBulkhead)
                        .withTimeLimiter(timeLimiter, scheduler)
                        .withCircuitBreaker(circuitBreaker)
                        .get()
                        .toCompletableFuture();
                observableEmitter.onNext(future.get());
                observableEmitter.onComplete();
            } catch (Exception e) {
                if (e instanceof ExecutionException) {
                    observableEmitter.onError(processException((ExecutionException) e));
                } else {
                    observableEmitter.onError(e);
                }
            }
        });
    }

    private RuntimeException processException(ExecutionException e) {
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
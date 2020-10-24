package io.github.harishb2k.easy.resilience;

import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.exception.ExceptionUtil;
import io.github.harishb2k.easy.resilience.exception.ResilienceException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.reactivex.Observable;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.github.harishb2k.easy.resilience.exception.ExceptionUtil.unwrapResilience4jException;
import static io.github.harishb2k.easy.resilience.exception.ExceptionUtil.unwrapResilience4jExecutionException;

public class ResilienceProcessor implements IResilienceProcessor {
    private CircuitBreaker circuitBreaker;
    private ThreadPoolBulkhead threadPoolBulkhead;
    private ScheduledExecutorService scheduler;
    private TimeLimiter timeLimiter;

    @Override
    public void initialized(ResilienceCallConfig config) {

        // Setup a circuit breaker with default settings
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
        circuitBreaker = CircuitBreaker.of(config.getId(), circuitBreakerConfig);

        // Setup a bulkhead for this request
        ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(config.getConcurrency())
                .maxThreadPoolSize(config.getConcurrency())
                .queueCapacity(config.getQueueSize())
                .build();
        threadPoolBulkhead = ThreadPoolBulkhead.of(config.getId(), threadPoolBulkheadConfig);

        // A scheduler and time limiter to handle timeouts
        scheduler = Executors.newScheduledThreadPool(config.getConcurrency());
        timeLimiter = TimeLimiter.of(Duration.ofMillis(config.getTimeout()));
    }

    @Override
    public <T> T execute(String id, Callable<T> callable, Class<T> cls) throws ResilienceException {
        try {
            CompletableFuture<T> future = Decorators
                    .ofCallable(callable)
                    .withThreadPoolBulkhead(threadPoolBulkhead)
                    .withTimeLimiter(timeLimiter, scheduler)
                    .withCircuitBreaker(circuitBreaker)
                    .get()
                    .toCompletableFuture();
            return future.get();
        } catch (ExecutionException e) {
            throw unwrapResilience4jExecutionException(e);
        } catch (Exception e) {
            throw unwrapResilience4jException(e);
        }
    }

    @Override
    public <T> Observable<T> executeAsObservable(String id, Callable<T> callable, Class<T> cls) {
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

    public <T> Observable<T> executeAsObservable(String id, Observable<T> observable, Class<T> cls) {
        return Observable.create(observableEmitter -> {

            Decorators.ofSupplier(observable::blockingFirst)
                    .withCircuitBreaker(circuitBreaker)
                    .withThreadPoolBulkhead(threadPoolBulkhead)
                    .withTimeLimiter(timeLimiter, scheduler)
                    .decorate()
                    .get()
                    .whenComplete((t, throwable) -> {
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
                    });
        });
    }
}
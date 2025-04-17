package io.github.devlibx.easy.resilience;

import io.github.devlibx.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.devlibx.easy.resilience.exception.ResilienceException;
import io.reactivex.rxjava3.core.Observable;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * A No-Operation implementation of {@link IResilienceProcessor} that simply passes through calls
 * without adding any resilience features. This implementation can be used in scenarios where
 * resilience features are not needed but the interface contract must be satisfied.
 */
@Slf4j
public class NoOpResilienceProcessor implements IResilienceProcessor {

    private ResilienceCallConfig config;

    /**
     * Stores the configuration but doesn't use it for any processing.
     */
    @Override
    public void initialized(ResilienceCallConfig config) {
        this.config = config;
        log.info("Initialized NoOpResilienceProcessor with id: {}", config.getId());
    }

    /**
     * Simply executes the callable directly without any resilience features.
     *
     * @param id       The identifier for this execution
     * @param callable The callable to execute
     * @param cls      The expected return type class
     * @return The result of the callable execution
     * @throws ResilienceException if the callable throws an exception
     */
    @Override
    public <T> T execute(String id, Callable<T> callable, Class<T> cls) throws ResilienceException {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new ResilienceException("Error executing callable in NoOpResilienceProcessor", e);
        }
    }

    /**
     * Converts the callable to an Observable and returns it.
     *
     * @param id       The identifier for this execution
     * @param callable The callable to execute
     * @param cls      The expected return type class
     * @return An Observable that emits the result of the callable
     */
    @Override
    public <T> Observable<T> executeObservable(String id, Callable<T> callable, Class<T> cls) {
        return Observable.fromCallable(callable);
    }

    /**
     * Simply returns the provided Observable without any resilience features.
     *
     * @param id         The identifier for this execution
     * @param observable The Observable to execute
     * @param cls        The expected return type class
     * @return The provided Observable without modification
     */
    @Override
    public <T> Observable<T> executeObservable(String id, Observable<T> observable, Class<T> cls) {
        return observable;
    }
}

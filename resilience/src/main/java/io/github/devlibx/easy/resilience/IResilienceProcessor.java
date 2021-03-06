package io.github.devlibx.easy.resilience;

import io.github.devlibx.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.devlibx.easy.resilience.exception.ResilienceException;
import io.reactivex.rxjava3.core.Observable;

import java.util.concurrent.Callable;

public interface IResilienceProcessor {

    /**
     * Setup processor
     */
    void initialized(ResilienceCallConfig config);

    /**
     * Execute a request
     *
     * @throws ResilienceException if there is a error on execution
     */
    <T> T execute(String id, Callable<T> callable, Class<T> cls) throws ResilienceException;

    /**
     * Execute a callable as observable
     */
    <T> Observable<T> executeObservable(String id, Callable<T> callable, Class<T> cls);

    /**
     * Execute a observable
     */
    <T> Observable<T> executeObservable(String id, Observable<T> observable, Class<T> cls);
}

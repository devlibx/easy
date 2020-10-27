package io.github.harishb2k.easy.http.util;

import com.google.inject.Key;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyBadRequestException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyHttpRequestException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyInternalServerErrorException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.github.harishb2k.easy.http.module.Async;
import io.github.harishb2k.easy.http.module.Sync;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.resilience.IResilienceManager;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.IResilienceProcessor;
import io.github.harishb2k.easy.resilience.ResilienceManager;
import io.reactivex.rxjava3.core.Observable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.harishb2k.easy.http.exception.EasyHttpExceptions.easyEasyResilienceException;

@SuppressWarnings({"EmptyTryBlock", "CatchMayIgnoreException", "ConstantConditions"})
@Slf4j
public class EasyHttp {
    /**
     * Map of all processor
     */
    private static final Map<String, IRequestProcessor> requestProcessors = new HashMap<>();
    private static final Map<String, IResilienceProcessor> resilienceProcessors = new HashMap<>();
    private static IResilienceManager resilienceManager;
    private static final Lock resilienceManagerLock = new ReentrantLock();

    /**
     * Free all resources
     */
    public static void shutdown() {
        requestProcessors.forEach((key, requestProcessor) -> Safe.safe(requestProcessor::shutdown));
        requestProcessors.clear();
        resilienceProcessors.clear();
        resilienceManager = null;
    }

    /**
     * Setup EasyHttp to make HTTP requests
     */
    public static void setup(Config config) {

        // Make server registry
        ServerRegistry serverRegistry = ApplicationContext.getInstance(ServerRegistry.class);
        serverRegistry.configure(config);

        // Make api registry
        ApiRegistry apiRegistry = ApplicationContext.getInstance(ApiRegistry.class);
        apiRegistry.configure(config);

        // Make sure we have resilienceManager object created
        ensureResilienceManager();

        // Setup all request processors
        serverRegistry.getServerMap().forEach((serverName, server) -> {
            apiRegistry.getApiMap().forEach((apiName, api) -> {

                // Key to be used for this API
                String key = serverName + "-" + apiName;

                // Build a request processor
                IRequestProcessor requestProcessor = null;
                if (api.isAsync()) {
                    requestProcessor = ApplicationContext.getInstance(Key.get(IRequestProcessor.class, Async.class));
                } else {
                    requestProcessor = ApplicationContext.getInstance(Key.get(IRequestProcessor.class, Sync.class));

                }
                requestProcessors.put(key, requestProcessor);

                // Setup resilience processor
                ResilienceCallConfig callConfig = ResilienceCallConfig.withDefaults()
                        .id(key)
                        .concurrency(api.getConcurrency())
                        .timeout(api.getTimeout())
                        .queueSize(api.getQueueSize())
                        .useSemaphore(api.isAsync())
                        .build();
                IResilienceProcessor resilienceProcessor = resilienceManager.getOrCreate(callConfig);
                resilienceProcessors.put(key, resilienceProcessor);

            });
        });

        // Warm-up connections and threads
        serverRegistry.getServerMap().forEach((serverName, server) -> {
            apiRegistry.getApiMap().forEach((apiName, api) -> {
                if (api.isNoWarmUp() || true) {
                    log.debug("service={} api={} warm-up is disabled. The very first call to {}.{} may timeout or fail if api timeout is small", server, api, server, api);
                    return;
                }
                try {
                    log.debug("making a warm-up call to service={} api={} - you may see bad api call in server logs", server, api);
                    EasyHttp.callSync(
                            Call.builder(Map.class)
                                    .withServerAndApi(serverName, apiName)
                                    .build()
                    );
                } catch (Exception ignored) {
                }
            });
        });
    }

    /**
     * Make a HTTP call which returns a response.
     * </br>
     * Note - to check for timeout errors you must catch both
     * {@link EasyResilienceRequestTimeoutException} and {@link EasyRequestTimeOutException}
     *
     * @param call request object
     * @param <T>  type of response
     * @return response of http call
     * @throws EasyHttpRequestException if error, it provides {@link EasyHttpRequestException}. You can catch specific
     *                                  type of errors by caching sub-class of EasyHttpRequestException.
     *                                  e.g. {@link EasyInternalServerErrorException}, {@link EasyBadRequestException}
     *                                  <p>
     *                                  It also throws {@link EasyResilienceException} which is also a sub-class of
     *                                  {@link EasyHttpRequestException}. These resilience exception are thrown when
     *                                  circuit is open, too many calls are made, or request timed out.
     *                                  <p>
     */
    public static <T> T callSync(Call<T> call) {
        try {
            return internalCall(call).blockingFirst();
        } catch (EasyResilienceException e) {
            Optional<EasyResilienceException> ex = easyEasyResilienceException(e);
            if (ex.isPresent()) {
                throw ex.get();
            } else {
                throw new EasyHttpRequestException(e);
            }
        } catch (EasyHttpRequestException e) {
            throw e;
        } catch (Exception e) {
            throw easyEasyResilienceException(e).orElseThrow(() -> new RuntimeException(e));
        }
    }

    /**
     * Make a HTTP call which returns a observable.
     * </br>
     * Note - to check for timeout errors you must catch both
     * {@link EasyResilienceRequestTimeoutException} and {@link EasyRequestTimeOutException}
     * <p>
     *
     * @param call request object
     * @param <T>  type of response
     * @return observable for response of http call
     * @throws EasyHttpRequestException <b color='red'>(exceptions will be received in the onError callback in subscriber)</b>
     *                                  if error, it provides {@link EasyHttpRequestException}. You can catch specific
     *                                  type of errors by caching sub-class of EasyHttpRequestException.
     *                                  e.g. {@link EasyInternalServerErrorException}, {@link EasyBadRequestException}
     *                                  <p>
     *                                  It also throws {@link EasyResilienceException} which is also a sub-class of
     *                                  {@link EasyHttpRequestException}. These resilience exception are thrown when
     *                                  circuit is open, too many calls are made, or request timed out.
     *                                  <p>
     */
    public static <T> Observable<T> callAsync(Call<T> call) {
        return Observable.create(observableEmitter -> {
            internalCall(call)
                    .subscribe(
                            t -> {
                                observableEmitter.onNext(t);
                                observableEmitter.onComplete();
                            },
                            throwable -> {
                                Exception e;
                                if (throwable instanceof EasyResilienceException) {
                                    Optional<EasyResilienceException> ex = easyEasyResilienceException(throwable);
                                    if (ex.isPresent()) {
                                        e = ex.get();
                                    } else {
                                        e = new EasyHttpRequestException(throwable);
                                    }
                                } else if (throwable instanceof EasyHttpRequestException) {
                                    e = (EasyHttpRequestException) throwable;
                                } else {
                                    Optional<EasyResilienceException> err = easyEasyResilienceException(throwable);
                                    if (err.isPresent()) {
                                        e = err.get();
                                    } else {
                                        e = new RuntimeException(throwable);
                                    }
                                }
                                observableEmitter.onError(e);
                            })
                    .dispose();
        });
    }

    /**
     * Call a HTTP Api. This API is wrapped in other convenience method to be used.
     */
    private static <T> Observable<T> internalCall(Call<T> call) {

        final String server = call.getServer();
        final String api = call.getApi();
        final String key = server + "-" + api;

        // Make sure we have server and api registered
        if (requestProcessors.get(key) == null) {
            return Observable.error(new RuntimeException("server=" + server + " api=" + api + " is not registered"));
        }

        // Build request
        RequestObject requestObject = new RequestObject();
        requestObject.setServer(server);
        requestObject.setApi(api);
        requestObject.setPathParam(call.getPathParams());
        requestObject.setQueryParam(call.getQueryParam());
        requestObject.setHeaders(call.getHeaders());
        requestObject.setBody(call.getBodyAsByteArray());

        // Build a Observable and process it to give final response (in flat map)
        Observable<T> observable = requestProcessors.get(server + "-" + api)
                .process(requestObject)
                .flatMap(responseObject -> {
                    T objectToReturn = call.getResponseBuilder().apply(responseObject.getBody());
                    return Observable.just(objectToReturn);
                });

        // Run it with resilience processor;
        return resilienceProcessors.get(key)
                .executeObservable(
                        key,
                        observable,
                        call.getResponseClass()
                );
    }

    // Make sure we have initialized resilienceManager
    private static void ensureResilienceManager() {
        if (resilienceManager == null) {
            resilienceManagerLock.lock();
            if (resilienceManager == null) {
                try {
                    resilienceManager = ApplicationContext.getInstance(IResilienceManager.class);
                } catch (Exception e) {
                    resilienceManager = new ResilienceManager();
                }
            }
            resilienceManagerLock.unlock();
        }
    }
}

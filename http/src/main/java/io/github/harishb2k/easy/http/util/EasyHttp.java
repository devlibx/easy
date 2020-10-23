package io.github.harishb2k.easy.http.util;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyHttpRequestException;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;
import io.github.harishb2k.easy.resilience.IResilienceManager;
import io.github.harishb2k.easy.resilience.IResilienceManager.IResilienceProcessor;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.ResilienceManager;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class EasyHttp {
    /**
     * Map of all processor
     */
    private static final Map<String, IRequestProcessor> requestProcessors = new HashMap<>();
    private static final Map<String, IResilienceProcessor> resilienceProcessors = new HashMap<>();

    private static IResilienceManager resilienceManager;
    private static Lock resilienceManagerLock = new ReentrantLock();

    /**
     * Free all resources
     */
    public static void shutdown() {
        requestProcessors.forEach((key, requestProcessor) -> Safe.safe(requestProcessor::shutdown));
    }

    public static void setup(Config config) {

        // Make server registry
        ServerRegistry serverRegistry = new ServerRegistry();
        serverRegistry.configure(config);

        // Make api registry
        ApiRegistry apiRegistry = new ApiRegistry();
        apiRegistry.configure(config);

        // Make sure we have resilienceManager object created
        ensureResilienceManager();

        // Setup all request processors
        serverRegistry.getServerMap().forEach((serverName, server) -> {
            apiRegistry.getApiMap().forEach((apiName, api) -> {
                IRequestProcessor requestProcessor;
                if (api.isAsync()) {
                    requestProcessor = new SyncRequestProcessor(serverRegistry, apiRegistry);
                } else {
                    requestProcessor = new SyncRequestProcessor(serverRegistry, apiRegistry);
                }
                requestProcessors.put(serverName + "-" + apiName, requestProcessor);

                ResilienceCallConfig callConfig = ResilienceCallConfig.withDefaults()
                        .id(serverName + "-" + apiName)
                        .concurrency(api.getConcurrency())
                        .timeout(api.getTimeout() + 1000)
                        .queueSize(api.getQueueSize())
                        .build();
                IResilienceProcessor resilienceProcessor = resilienceManager.getOrCreate(callConfig);
                resilienceProcessors.put(serverName + "-" + apiName, resilienceProcessor);

            });
        });
    }

    public static <T> T callSync(String server,
                                 String api,
                                 Map<String, Object> pathParam,
                                 MultivaluedMap<String, Object> queryParam,
                                 Map<String, Object> headers,
                                 Object body,
                                 Class<T> cls
    ) {
        try {
            return call(
                    server,
                    api,
                    pathParam,
                    queryParam,
                    headers,
                    body,
                    cls
            ).blockingFirst();
        } catch (Exception e) {
            throw resilienceManager.processException(e);
        }
    }

    public static <T> Observable<T> call(String server,
                                         String api,
                                         Map<String, Object> pathParam,
                                         MultivaluedMap<String, Object> queryParam,
                                         Map<String, Object> headers,
                                         Object body,
                                         Class<T> cls
    ) {

        final String key = server + "-" + api;

        // Make sure we have server and api registered
        if (requestProcessors.get(key) == null) {
            return Observable.error(new RuntimeException("server=" + server + " api=" + api + " is not registered"));
        }

        // Get resilienceManager object before we start anything
        ensureResilienceManager();

        // Build request
        RequestObject requestObject = new RequestObject();
        requestObject.setServer(server);
        requestObject.setApi(api);
        requestObject.setPathParam(pathParam);
        requestObject.setQueryParam(queryParam);
        requestObject.setHeaders(headers);
        requestObject.setBody(body);

        Observable<T> observable = requestProcessors.get(server + "-" + api).process(requestObject)
                .flatMap((Function<ResponseObject, ObservableSource<T>>) responseObject -> {
                    // Get body
                    String bodyString = null;
                    if (responseObject.getBody() != null) {
                        bodyString = new String(responseObject.getBody());
                    }

                    // Convert to requested class
                    T objectToReturn = JsonUtils.readObject(bodyString, cls);
                    return Observable.just(objectToReturn);
                });

        return resilienceProcessors.get(key).executeAsObservable(
                key,
                observable,
                cls
        );

      /*  return requestProcessors.get(server + "-" + api).process(requestObject)
                .flatMap((Function<ResponseObject, ObservableSource<T>>) responseObject -> {
                    // Get body
                    String bodyString = null;
                    if (responseObject.getBody() != null) {
                        bodyString = new String(responseObject.getBody());
                    }

                    // Convert to requested class
                    T objectToReturn = JsonUtils.readObject(bodyString, cls);
                    return Observable.just(objectToReturn);
                });*/
    }

    public static EasyHttpRequestException convertException(Throwable throwable) {
        if (throwable instanceof EasyHttpRequestException) {
            return (EasyHttpRequestException) throwable;
        } else {
            return new EasyHttpRequestException(throwable);
        }
    }

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

package io.github.harishb2k.easy.http.util;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.async.AsyncRequestProcessor;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.resilience.IResilienceManager;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.IResilienceProcessor;
import io.github.harishb2k.easy.resilience.ResilienceManager;
import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    }

    /**
     * Setup EasyHttp to make HTTP requests
     */
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

                // Key to be used for this API
                String key = serverName + "-" + apiName;

                // Build a request processor
                IRequestProcessor requestProcessor = null;
                if (api.isAsync()) {
                    try {
                        // requestProcessor = ApplicationContext.getInstance(AsyncRequestProcessor.class);
                    } catch (Exception e) {
                    }
                    if (requestProcessor == null) {
                        requestProcessor = new AsyncRequestProcessor(serverRegistry, apiRegistry);
                    }
                } else {
                    try {
                        // requestProcessor = ApplicationContext.getInstance(SyncRequestProcessor.class);
                    } catch (Exception e) {
                    }
                    if (requestProcessor == null) {
                        requestProcessor = new AsyncRequestProcessor(serverRegistry, apiRegistry);
                    }
                }
                requestProcessors.put(key, requestProcessor);

                // Setup resilience processor
                ResilienceCallConfig callConfig = ResilienceCallConfig.withDefaults()
                        .id(key)
                        .concurrency(api.getConcurrency())
                        .timeout(api.getTimeout())
                        .queueSize(api.getQueueSize())
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
                            serverName,
                            apiName,
                            null,
                            null,
                            null,
                            null,
                            Map.class
                    );
                } catch (Exception ignored) {
                }
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
        return call(
                server,
                api,
                pathParam,
                queryParam,
                headers,
                body,
                cls
        ).blockingFirst();
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

        // Build request
        RequestObject requestObject = new RequestObject();
        requestObject.setServer(server);
        requestObject.setApi(api);
        requestObject.setPathParam(pathParam);
        requestObject.setQueryParam(queryParam);
        requestObject.setHeaders(headers);
        requestObject.setBody(body);

        // Build a Observable and process it to give final response (in flat map)
        Observable<T> observable = requestProcessors.get(server + "-" + api)
                .process(requestObject)
                .flatMap(responseObject -> {

                    // Get body
                    String bodyString = null;
                    if (responseObject.getBody() != null) {
                        bodyString = new String(responseObject.getBody());
                    }

                    // Convert to requested class
                    T objectToReturn = JsonUtils.readObject(bodyString, cls);
                    return Observable.just(objectToReturn);

                });

        // Run it with resilience processor;
        return resilienceProcessors.get(key)
                .executeObservable(
                        key,
                        observable,
                        cls
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

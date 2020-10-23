package io.github.harishb2k.easy.http.util;

import io.gitbub.harishb2k.easy.helper.Safe;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EasyHttp {
    /**
     * Map of all processor
     */
    private static final Map<String, IRequestProcessor> requestProcessors = new HashMap<>();

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
            });
        });
    }

    public static <T> Observable<T> call(String server,
                                         String api,
                                         Map<String, Object> pathParam,
                                         MultivaluedMap<String, Object> queryParam,
                                         Map<String, Object> headers,
                                         Object body,
                                         Class<T> cls
    ) {

        // Make sure we have server and api registered
        if (requestProcessors.get(server + "-" + api) == null) {
            return Observable.error(new RuntimeException("server=" + server + " api=" + api + " is not registered"));
        }

        RequestObject requestObject = new RequestObject();
        requestObject.setServer(server);
        requestObject.setApi(api);
        requestObject.setPathParam(pathParam);
        requestObject.setQueryParam(queryParam);
        requestObject.setHeaders(headers);
        requestObject.setBody(body);

        return requestProcessors.get(server + "-" + api).process(requestObject)
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
    }
}

package io.github.harishb2k.easy.http.util;

import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;
import io.reactivex.Observable;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

public class EasyHttpHelper {
    private static final Map<String, IRequestProcessor> requestProcessors = new HashMap<>();

    public static void setup(Config config, StringHelper stringHelper) {

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
                    requestProcessor = new SyncRequestProcessor(serverRegistry, apiRegistry, stringHelper);
                } else {
                    requestProcessor = new SyncRequestProcessor(serverRegistry, apiRegistry, stringHelper);
                }
                requestProcessors.put(serverName + "-" + apiName, requestProcessor);
            });
        });
    }

    // private String server;
    //    private String api;
    //    private String method = "GET";
    //    private Map<String, Object> headers;
    //    private Map<String, Object> pathParam;
    //    private MultivaluedMap<String, Object> queryParam;
    //    private Object body;

    public static <T> Observable<T> call(String server,
                                         String api,
                                         Map<String, Object> pathParam,
                                         MultivaluedMap<String, Object> queryParam,
                                         Map<String, Object> headers,
                                         Object body,
                                         Class<T> cls
    ) {
        RequestObject requestObject = new RequestObject();
        requestObject.setServer(server);
        requestObject.setApi(api);
        requestObject.setPathParam(pathParam);
        requestObject.setQueryParam(queryParam);
        requestObject.setHeaders(headers);
        requestObject.setBody(body);
        return Observable.create(observable -> {
            ResponseObject responseObject = requestProcessors.get(server + "-" + api).process(requestObject);
            T t = JsonUtils.readObject(new String(responseObject.getBody()), cls);
            observable.onNext(t);
            observable.onComplete();
        });
    }
}

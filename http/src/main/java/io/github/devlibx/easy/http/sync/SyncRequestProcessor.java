package io.github.devlibx.easy.http.sync;

import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import io.github.devlibx.easy.http.IRequestProcessor;
import io.github.devlibx.easy.http.RequestObject;
import io.github.devlibx.easy.http.ResponseObject;
import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Server;
import io.github.devlibx.easy.http.registry.ApiRegistry;
import io.github.devlibx.easy.http.registry.ServerRegistry;
import io.reactivex.rxjava3.core.Observable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@SuppressWarnings("FieldMayBeFinal")
public class SyncRequestProcessor implements IRequestProcessor {
    private final ServerRegistry serverRegistry;
    private final ApiRegistry apiRegistry;
    private final StringHelper stringHelper;
    private final IHttpResponseProcessor httpResponseProcessor;
    private final IMetrics metrics;

    @com.google.inject.Inject
    @Inject
    public SyncRequestProcessor(ServerRegistry serverRegistry, ApiRegistry apiRegistry, IHttpResponseProcessor httpResponseProcessor, IMetrics metrics) {
        this.serverRegistry = serverRegistry;
        this.apiRegistry = apiRegistry;
        this.httpResponseProcessor = httpResponseProcessor;
        this.metrics = metrics;
        this.stringHelper = ApplicationContext.getOptionalInstance(StringHelper.class).orElse(new StringHelper());
    }

    @Override
    public void shutdown() {
        Safe.safe(apiRegistry::shutdown);
        Safe.safe(serverRegistry::shutdown);
    }

    @Override
    public Observable<ResponseObject> process(RequestObject requestObject) {

        // Get api and server from registry
        final Api api = apiRegistry.getOptional(requestObject.getApi()).orElseThrow(() -> new RuntimeException("Could not find api=" + requestObject.getApi()));
        final Server server = serverRegistry.getOptional(api.getServer()).orElseThrow(() -> new RuntimeException("Could not find server=" + api.getServer()));

        // Set correct type of method in request from API
        if (Strings.isNullOrEmpty(requestObject.getMethod())) {
            requestObject.setMethod(api.getMethod());
        }

        // Build a observer to handle this request
        return Observable.create(observableEmitter -> {

            try {

                // Primary Path - request and give result
                ResponseObject responseObject = internalProcess(server, api, requestObject);
                if (responseObject != null) {
                    observableEmitter.onNext(responseObject);
                }
                observableEmitter.onComplete();

            } catch (Exception e) {

                if (!Strings.isNullOrEmpty(api.getFallbackApiName())) {
                    // Secondary flow to handle fallback API - If Primary path failed and we have a fallback configured
                    // then use it

                    // Get api and server from registry
                    log.info("Going to fallback: server={}, api={}, fallbackApi={}", server.getName(), api.getName(), api.getFallbackApiName());
                    final Api fallbackApi = apiRegistry.getOptional(api.getFallbackApiName()).orElseThrow(() -> new RuntimeException("Could not find fallback api=" + api.getFallbackApiName()));
                    final Server fallbackServer = serverRegistry.getOptional(fallbackApi.getServer()).orElseThrow(() -> new RuntimeException("Could not find fallback server=" + fallbackApi.getServer()));

                    // Set correct type of method in request from API
                    if (Strings.isNullOrEmpty(requestObject.getMethod())) {
                        requestObject.setMethod(api.getMethod());
                    }

                    // Try to process this request by fallback
                    try {
                        ResponseObject responseObject = internalProcess(fallbackServer, fallbackApi, requestObject);
                        if (responseObject != null) {
                            observableEmitter.onNext(responseObject);
                        }
                        observableEmitter.onComplete();
                    } catch (Exception e1) {
                        observableEmitter.onError(e1);
                    }

                } else {

                    // No fallback is set - send back the error
                    observableEmitter.onError(e);
                }
            }
        });
    }

    @SuppressWarnings({"Convert2MethodRef", "UnnecessaryLocalVariable"})
    private ResponseObject internalProcess(Server server, Api api, RequestObject requestObject) {
        switch (requestObject.getMethod()) {
            case "GET":
                return internalProcess(server, api, requestObject, uri -> {
                    HttpGet get = new HttpGet(uri);
                    return get;
                }, HttpGet.class);
            case "POST":
                return internalProcess(server, api, requestObject, uri -> {
                    HttpPost post = new HttpPost(uri);
                    if (requestObject.getBody() != null) {
                        post.setEntity(new ByteArrayEntity(requestObject.getBody()));
                    }
                    return post;
                }, HttpPost.class);
            case "PUT":
                return internalProcess(server, api, requestObject, uri -> {
                    HttpPut put = new HttpPut(uri);
                    if (requestObject.getBody() != null) {
                        put.setEntity(new ByteArrayEntity(requestObject.getBody()));
                    }
                    return put;
                }, HttpPut.class);
            case "DELETE":
                return internalProcess(server, api, requestObject, uri -> {
                    HttpDelete delete = new HttpDelete(uri);
                    return delete;
                }, HttpDelete.class);
        }
        return null;
    }

    @SuppressWarnings({"EmptyTryBlock", "TryWithIdenticalCatches"})
    private <REQ_TYPE extends HttpRequestBase> ResponseObject internalProcess(Server server, Api api, RequestObject requestObject, Function<URI, REQ_TYPE> func, Class<REQ_TYPE> cls) {

        // Build a URL - replace path param and add query params
        URI uri;
        try {
            uri = generateURI(server, api, requestObject);
            log.debug("URL to use = {}", uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to generate URI");
        }

        // Make a http request
        HttpRequestBase requestBase = func.apply(uri);
        requestBase.setConfig(buildRequestConfig(server, api, requestObject));

        // Take headers from server
        if (server.getHeaders() != null) {
            server.getHeaders().forEach((key, value) -> {
                requestBase.addHeader(key, stringHelper.stringify(value));
            });
        }

        // Add headers to the request
        requestObject.preProcessHeaders(api.getHeaders());
        requestObject.getHeaders().forEach((key, value) -> {
            requestBase.addHeader(key, stringHelper.stringify(value));
        });

        // Get a http client to make request
        CloseableHttpClient client = apiRegistry.getClient(server, api, CloseableHttpClient.class);

        // Request server
        ResponseObject responseObject;
        long startTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = client.execute(requestBase)) {
            responseObject = httpResponseProcessor.process(serverRegistry.get(api.getServer()), api, response);
            metrics.observe(server.getName() + "_" + api.getName() + "_http_client_time", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            metrics.observe(server.getName() + "_" + api.getName() + "_http_client_error_time", System.currentTimeMillis() - startTime);
            log.error("Unknown issue: request={}", requestObject, e);
            responseObject = httpResponseProcessor.processException(server, api, e);
        }

        if (requestObject.getResponseBuilder() == null) {
            log.debug("Request={} Response={}", requestObject, responseObject.convertAsMap());
        } else {
            try {
                log.debug("Request={} Response={}", requestObject, requestObject.getResponseBuilder().apply(responseObject.getBody()));
            } catch (Exception e) {
                log.debug("Request={} Response={}", requestObject, "Failed to build response from body using responseBuilder function", e);
            }
        }

        // Throw correct exception if required
        httpResponseProcessor.processResponseForException(responseObject);

        return responseObject;
    }

    private RequestConfig buildRequestConfig(Server server, Api api, RequestObject request) {
        int socketTimeoutToBeUsed = api.getTimeout();
        if (api.getTimeoutDeltaFactor() > 0) {
            socketTimeoutToBeUsed = (int) (socketTimeoutToBeUsed + (api.getTimeoutDeltaFactor() * socketTimeoutToBeUsed));
        }
        return RequestConfig.custom().setConnectTimeout(server.getConnectTimeout()).setConnectionRequestTimeout(server.getConnectionRequestTimeout()).setSocketTimeout(socketTimeoutToBeUsed).build();
    }

    private URI generateURI(Server server, Api api, RequestObject request) throws URISyntaxException {
        if (server.getPort() == -1) {
            return new URIBuilder().setScheme(server.isHttps() ? "https" : "http").setHost(server.getHost()).setPath(resolvePath(server, api, request)).setParameters(getQueryParams(server, api, request)).build();
        }
        return new URIBuilder().setScheme(server.isHttps() ? "https" : "http").setHost(server.getHost()).setPort(server.getPort()).setPath(resolvePath(server, api, request)).setParameters(getQueryParams(server, api, request)).build();
    }

    @SuppressWarnings("deprecation")
    private String resolvePath(Server server, Api api, RequestObject request) {
        String uri = api.getPath();
        if (!Strings.isNullOrEmpty(uri) && request.getPathParam() != null) {
            uri = StrSubstitutor.replace(api.getPath(), request.getPathParam());
        }
        if (Strings.isNullOrEmpty(uri)) {
            uri = "/";
        }
        return uri.startsWith("/") ? uri : "/" + uri;
    }

    private List<NameValuePair> getQueryParams(Server server, Api api, RequestObject request) {
        final List<NameValuePair> queryParams = new ArrayList<>();
        if (null != request.getQueryParam()) {
            request.getQueryParam().forEach((key, values) -> values.forEach(value -> queryParams.add(new BasicNameValuePair(key, stringHelper.stringify(value)))));
        }
        return queryParams;
    }
}

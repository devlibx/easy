package io.github.harishb2k.easy.http.sync;

import com.google.common.base.Strings;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
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

    @com.google.inject.Inject(optional = true)
    private IHttpResponseProcessor httpResponseProcessor = new DefaultHttpResponseProcessor();

    @Inject
    public SyncRequestProcessor(ServerRegistry serverRegistry, ApiRegistry apiRegistry, StringHelper stringHelper) {
        this.serverRegistry = serverRegistry;
        this.apiRegistry = apiRegistry;
        this.stringHelper = stringHelper;
    }

    @SuppressWarnings("Convert2MethodRef")
    @Override
    public ResponseObject process(RequestObject requestObject) {
        final Api api = apiRegistry.getOptional(requestObject.getApi()).orElseThrow(() -> new RuntimeException("Could not find api=" + requestObject.getApi()));
        final Server server = serverRegistry.getOptional(api.getServer()).orElseThrow(() -> new RuntimeException("Could not find server=" + api.getServer()));

        switch (requestObject.getMethod()) {
            case "GET":
                return internalProcess(server, api, requestObject, uri -> {
                    return new HttpGet(uri);
                }, HttpGet.class);
            case "POST":
                return internalProcess(server, api, requestObject, uri -> {
                    return new HttpPost(uri);
                }, HttpPost.class);
            case "PUT":
                return internalProcess(server, api, requestObject, uri -> {
                    return new HttpPut(uri);
                }, HttpPut.class);
            case "DELETE":
                return internalProcess(server, api, requestObject, uri -> {
                    return new HttpDelete(uri);
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

        // Add headers to the request
        requestObject.preProcessHeaders();
        requestObject.getHeaders().forEach((key, value) -> {
            requestBase.addHeader(key, stringHelper.stringify(value));
        });

        // Request server
        ResponseObject responseObject;
        CloseableHttpClient client = apiRegistry.getClient(server, api, CloseableHttpClient.class);
        try (CloseableHttpResponse response = client.execute(requestBase)) {
            responseObject = httpResponseProcessor.process(serverRegistry.get(api.getServer()), api, response);
        } catch (Exception e) {
            responseObject = httpResponseProcessor.processException(server, api, e);
        }

        log.debug("Request={} Response={}", requestObject, responseObject.convertAsMap());
       //  System.out.println(responseObject.convertAsMap());
        System.out.println("---");
        return responseObject;
    }

    private RequestConfig buildRequestConfig(Server server, Api api, RequestObject request) {
        int socketTimeoutToBeUsed = api.getTimeout();
        if (api.getTimeoutDeltaFactor() > 0) {
            socketTimeoutToBeUsed = (int) (socketTimeoutToBeUsed + (api.getTimeoutDeltaFactor() * socketTimeoutToBeUsed));
        }
        return RequestConfig.custom()
                .setConnectTimeout(api.getConnectTimeout())
                .setConnectionRequestTimeout(api.getConnectionRequestTimeout())
                .setSocketTimeout(socketTimeoutToBeUsed)
                .build();
    }

    private URI generateURI(Server server, Api api, RequestObject request) throws URISyntaxException {
        return new URIBuilder()
                .setScheme(server.isHttps() ? "https" : "http")
                .setHost(server.getHost())
                .setPort(server.getPort())
                .setPath(resolvePath(server, api, request))
                .setParameters(getQueryParams(server, api, request))
                .build();
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
            request.getQueryParam()
                    .forEach((key, values) -> values
                            .forEach(value -> queryParams.add(new BasicNameValuePair(key, stringHelper.stringify(value)))));
        }
        return queryParams;
    }
}

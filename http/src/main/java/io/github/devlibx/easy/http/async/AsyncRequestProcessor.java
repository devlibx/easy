package io.github.devlibx.easy.http.async;

import com.google.inject.Inject;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import io.github.devlibx.easy.http.IRequestProcessor;
import io.github.devlibx.easy.http.RequestObject;
import io.github.devlibx.easy.http.ResponseObject;
import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Server;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions.EasyHttpRequestException;
import io.github.devlibx.easy.http.registry.ApiRegistry;
import io.github.devlibx.easy.http.registry.ServerRegistry;
import io.github.devlibx.easy.http.sync.IHttpResponseProcessor;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

@Slf4j
public class AsyncRequestProcessor implements IRequestProcessor {
    private final ServerRegistry serverRegistry;
    private final ApiRegistry apiRegistry;
    private final StringHelper stringHelper;
    private final IHttpResponseProcessor httpResponseProcessor;
    private final IMetrics metrics;

    @Inject
    public AsyncRequestProcessor(ServerRegistry serverRegistry, ApiRegistry apiRegistry, IHttpResponseProcessor httpResponseProcessor, IMetrics metrics) {
        this.serverRegistry = serverRegistry;
        this.apiRegistry = apiRegistry;
        this.httpResponseProcessor = httpResponseProcessor;
        this.metrics = metrics;
        this.stringHelper = ApplicationContext.getOptionalInstance(StringHelper.class).orElse(new StringHelper());
    }


    @Override
    public Observable<ResponseObject> process(RequestObject requestObject) {
        return internalProcess(requestObject);
    }

    public Observable<ResponseObject> internalProcess(RequestObject requestObject) {

        // Get api and server from registry
        final Api api = apiRegistry.getOptional(requestObject.getApi()).orElseThrow(() -> new RuntimeException("Could not find api=" + requestObject.getApi()));
        final Server server = serverRegistry.getOptional(api.getServer()).orElseThrow(() -> new RuntimeException("Could not find server=" + api.getServer()));

        return Observable.create(observableEmitter -> {
            processRequest(server, api, requestObject, observableEmitter);
        });
    }

    public void processRequest(Server server, Api api, RequestObject requestObject, ObservableEmitter<ResponseObject> observableEmitter) {

        // Get a web client to process this request
        WebClient webClient = apiRegistry.getClient(server, api, WebClient.class);
        long startTime = System.currentTimeMillis();
        switch (api.getMethod()) {
            case "GET": {
                webClient
                        .get()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .headers(consumerHeaders(requestObject))
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .doOnError(onErrorConsumer(server, api, observableEmitter, startTime))
                        .subscribe(consumer(server, api, observableEmitter, startTime));
                break;
            }

            case "DELETE": {
                webClient
                        .delete()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .headers(consumerHeaders(requestObject))
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .doOnError(onErrorConsumer(server, api, observableEmitter, startTime))
                        .subscribe(consumer(server, api, observableEmitter, startTime));
                break;
            }

            case "POST": {
                webClient
                        .post()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .headers(consumerHeaders(requestObject))
                        .bodyValue(requestObject.getBody())
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .doOnError(onErrorConsumer(server, api, observableEmitter, startTime))
                        .subscribe(consumer(server, api, observableEmitter, startTime));
                break;
            }

            case "PUT": {
                System.out.println("Putting data " + new String(requestObject.getBody()));
                webClient
                        .put()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .headers(consumerHeaders(requestObject))
                        .bodyValue(requestObject.getBody())
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .doOnError(onErrorConsumer(server, api, observableEmitter, startTime))
                        .subscribe(consumer(server, api, observableEmitter, startTime));
                break;
            }

            default:
                observableEmitter.onError(new RuntimeException("Api has a invalid HTTP method: " + api.getMethod()));
        }
    }

    private Consumer<byte[]> consumer(Server server, Api api, ObservableEmitter<ResponseObject> observableEmitter, long startTime) {
        return data -> {
            // Log time taken by http client
            metrics.observe(server.getName() + "_" + api.getName() + "_http_client_time", System.currentTimeMillis() - startTime);

            ResponseObject responseObject = new ResponseObject();
            responseObject.setBody(data);
            responseObject.setStatusCode(200);
            observableEmitter.onNext(responseObject);
            observableEmitter.onComplete();
        };
    }

    private Consumer<Throwable> onErrorConsumer(Server server, Api api, ObservableEmitter<ResponseObject> observableEmitter, long startTime) {
        return throwable -> {
            // Log time taken by http client
            metrics.observe(server.getName() + "_" + api.getName() + "_http_client_error_time", System.currentTimeMillis() - startTime);

            log.trace("Got error for server={}, api={}", server, api, throwable);
            ResponseObject responseObject = httpResponseProcessor.processException(server, api, throwable);
            EasyHttpRequestException exception = EasyHttpExceptions.convert(responseObject.getStatusCode(), throwable, responseObject);
            observableEmitter.onError(exception);
        };
    }

    private Consumer<HttpHeaders> consumerHeaders(RequestObject requestObject) {
        return httpHeaders -> {
            requestObject.preProcessHeaders();
            requestObject.getHeaders().forEach((key, value) -> {
                httpHeaders.add(key, stringHelper.stringify(value));
            });
        };
    }

    @Override
    public void shutdown() {
    }
}

package io.github.harishb2k.easy.http.async;

import com.google.inject.Inject;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

@Slf4j
public class AsyncRequestProcessor implements IRequestProcessor {
    private final ServerRegistry serverRegistry;
    private final ApiRegistry apiRegistry;
    private final StringHelper stringHelper;

    @Inject
    public AsyncRequestProcessor(ServerRegistry serverRegistry, ApiRegistry apiRegistry) {
        this.serverRegistry = serverRegistry;
        this.apiRegistry = apiRegistry;
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

        switch (api.getMethod()) {
            case "GET": {
                webClient
                        .get()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(onErrorConsumer(observableEmitter))
                        .subscribe(consumer(observableEmitter));
                break;
            }

            case "DELETE": {
                webClient
                        .delete()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(onErrorConsumer(observableEmitter))
                        .subscribe(consumer(observableEmitter));
                break;
            }

            case "POST": {
                webClient
                        .post()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .bodyValue(requestObject.getBody())
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(onErrorConsumer(observableEmitter))
                        .subscribe(consumer(observableEmitter));
                break;
            }

            case "PUT": {
                webClient
                        .put()
                        .uri(api.getUrlForRequestObject(requestObject, stringHelper))
                        .bodyValue(requestObject.getBody())
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(onErrorConsumer(observableEmitter))
                        .subscribe(consumer(observableEmitter));
                break;
            }

            default:
                throw new RuntimeException("Api has a invalid HTTP method: " + api.getMethod());
        }
    }

    private Consumer<String> consumer(ObservableEmitter<ResponseObject> observableEmitter) {
        return data -> {
            ResponseObject responseObject = new ResponseObject();
            responseObject.setBody(data.getBytes());
            responseObject.setStatusCode(200);
            observableEmitter.onNext(responseObject);
            observableEmitter.onComplete();
        };
    }

    private Consumer<Throwable> onErrorConsumer(ObservableEmitter<ResponseObject> observableEmitter) {
        return throwable -> {
            log.error("error", throwable);
            observableEmitter.onError(throwable);
        };
    }

    @Override
    public void shutdown() {
    }
}

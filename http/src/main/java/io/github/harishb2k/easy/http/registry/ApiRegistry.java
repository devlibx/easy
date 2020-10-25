package io.github.harishb2k.easy.http.registry;

import com.google.inject.Inject;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.helper.AsyncHttpClientBuilder;
import io.github.harishb2k.easy.http.helper.HttpClientBuilder;
import io.github.harishb2k.easy.http.helper.IClientBuilder;
import lombok.Getter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApiRegistry {
    @Getter
    private final Map<String, Api> apiMap;

    @SuppressWarnings("FieldMayBeFinal")
    @Inject
    private IClientBuilder httpClientBuilder = new HttpClientBuilder();

    @Inject
    private IClientBuilder asyncHttpClientBuilder = new AsyncHttpClientBuilder();

    public ApiRegistry() {
        this.apiMap = new HashMap<>();
    }

    public void configure(Config config) {
        apiMap.putAll(config.getApis());
    }

    public Optional<Api> getOptional(String api) {
        return Optional.ofNullable(apiMap.get(api));
    }

    @SuppressWarnings("unchecked")
    public <T> T getClient(Server server, Api api, Class<T> cls) {
        if (httpClientBuilder.accept(server, api)) {
            return (T) httpClientBuilder.buildClient(server, api, CloseableHttpClient.class);
        } else if (asyncHttpClientBuilder.accept(server, api)) {
            return (T) asyncHttpClientBuilder.buildClient(server, api, WebClient.class);
        }
        throw new RuntimeException("Request not supported");
    }

    /**
     * Shutdown builder
     */
    public void shutdown() {
        Safe.safe(() -> httpClientBuilder.shutdown());
    }
}

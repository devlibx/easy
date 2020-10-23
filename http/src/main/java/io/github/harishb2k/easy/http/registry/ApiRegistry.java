package io.github.harishb2k.easy.http.registry;

import com.google.inject.Inject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.registry.helper.HttpClientBuilder;
import io.github.harishb2k.easy.http.registry.helper.IClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApiRegistry {
    private final Map<String, Api> apiMap;

    @SuppressWarnings("FieldMayBeFinal")
    @Inject
    private IClientBuilder httpClientBuilder = new HttpClientBuilder();

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
        }
        throw new RuntimeException("Request not supported");
    }
}

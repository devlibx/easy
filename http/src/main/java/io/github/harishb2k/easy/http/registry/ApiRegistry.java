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

public class ApiRegistry {
    private final Map<String, Api> apiMap;
    private final Map<String, CloseableHttpClient> httpClientMap;

    @Inject
    private IClientBuilder httpClientBuilder = new HttpClientBuilder();

    public ApiRegistry() {
        this.apiMap = new HashMap<>();
        this.httpClientMap = new HashMap<>();
    }

    public void configure(Config config) {
        apiMap.putAll(config.getApis());
    }

    public Api get(String api) {
        return apiMap.get(api);
    }

    @SuppressWarnings("unchecked")
    public <T> T getClient(String apiName, Server server, Class<T> cls) {
        Api api = get(apiName);
        if ("HTTP".equals(api.getType())) {
            return (T) httpClientBuilder.buildClient(server, api, CloseableHttpClient.class);
        }
        return null;
    }
}

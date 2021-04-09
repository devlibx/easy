package io.github.devlibx.easy.http.helper;

import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Server;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class AsyncHttpClientBuilder implements IClientBuilder {
    private final Map<String, WebClient> httpClientMap;

    public AsyncHttpClientBuilder() {
        this.httpClientMap = new HashMap<>();
    }

    @Override
    public boolean accept(Server server, Api api) {
        if (!api.isAsync()) return false;
        return "HTTP".equals(api.getType()) || "HTTPS".equals(api.getType());
    }

    @Override
    public <T> T buildClient(Server server, Api api, Class<T> cls) {
        if (!cls.isAssignableFrom(WebClient.class)) {
            throw new RuntimeException("cls type must be CloseableHttpClient");
        }

        String key = server.getName() + "-" + api.getName();
        if (httpClientMap.containsKey(key)) {
            return (T) httpClientMap.get(key);
        }

        HttpClient httpClient = HttpClient.create(
                ConnectionProvider.create(
                        server.getName() + "-" + api.getName(),
                        api.getConcurrency()
                )
        ).tcpConfiguration(tcpClient ->
                tcpClient.doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(api.getTimeout(), TimeUnit.MILLISECONDS))
                )
        );
        WebClient webClient = WebClient.builder()
                .baseUrl(server.getUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        httpClientMap.put(key, webClient);
        return (T) webClient;
    }

    @Override
    public void shutdown() {
    }
}

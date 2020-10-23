package io.github.harishb2k.easy.http.registry.helper;

import io.gitbub.harishb2k.easy.helper.Safe;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;
import io.opentracing.contrib.apache.http.client.TracingHttpClientBuilder;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientBuilder implements IClientBuilder {
    private final ScheduledExecutorService executorService;
    private final Map<String, CloseableHttpClient> httpClientMap;

    public HttpClientBuilder() {
        this.httpClientMap = new ConcurrentHashMap<>();
        executorService = Executors.newScheduledThreadPool(2);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T buildClient(Server server, Api api, Class<T> cls) {
        if (!cls.isAssignableFrom(CloseableHttpClient.class)) {
            throw new RuntimeException("cls type must be CloseableHttpClient");
        }

        CloseableHttpClient client;
        if (!httpClientMap.containsKey(server.getName() + "-" + api.getName())) {
            client = buildHttpClient(server, api);
            httpClientMap.put(server.getName() + "-" + api.getName(), client);
        }
        return (T) httpClientMap.get(server.getName() + "-" + api.getName());
    }

    @Override
    public void close() {
        Safe.safe(executorService::shutdown);
        Safe.safe(() -> {
            httpClientMap.forEach((key, closeableHttpClient) -> {
                log.info("Closing connections: key={}", key);
                Safe.safe(() -> {
                    try {
                        closeableHttpClient.close();
                    } catch (Exception ignored) {
                    }
                });
            });
        });
    }

    public CloseableHttpClient buildHttpClient(Server server, Api api) {
        PoolingHttpClientConnectionManager connectionManager;
        if (server.getPollingConnectionTtlInMs() > 0) {
            connectionManager = new PoolingHttpClientConnectionManager(server.getPollingConnectionTtlInMs(), TimeUnit.MILLISECONDS);
        } else {
            connectionManager = new PoolingHttpClientConnectionManager();
        }
        connectionManager.setMaxTotal(api.getConcurrency());
        connectionManager.setDefaultMaxPerRoute(api.getConcurrency());

        executorService.scheduleWithFixedDelay(() -> {
            connectionManager.closeExpiredConnections();
            connectionManager.closeIdleConnections(server.getIdleConnectionTimeoutInSec(), TimeUnit.SECONDS);
        }, 10, 30, TimeUnit.SECONDS);

        TracingHttpClientBuilder builder = new TracingHttpClientBuilder().withTracer(GlobalTracer.get());
        builder.setDefaultRequestConfig(RequestConfig.custom().setRedirectsEnabled(true).build());
        builder.setConnectionManager(connectionManager);
        return builder.build();
    }
}

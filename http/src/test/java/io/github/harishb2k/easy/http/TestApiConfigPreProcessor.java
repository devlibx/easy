package io.github.devlibx.easy.http;

import io.github.devlibx.easy.http.config.Api;

public class TestApiConfigPreProcessor implements IApiConfigPreProcessor {
    @Override
    public void process(String name, Api api) {
        api.setQueueSize(123);
    }
}

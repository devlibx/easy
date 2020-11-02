package io.github.harishb2k.easy.http;

import io.github.harishb2k.easy.http.config.Api;

public class TestApiConfigPreProcessor implements IApiConfigPreProcessor {
    @Override
    public void process(String name, Api api) {
        api.setQueueSize(123);
    }
}

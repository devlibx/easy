package io.github.devlibx.easy.http;

import io.github.devlibx.easy.http.config.Api;

public interface IApiConfigPreProcessor {
    void process(String name, Api api);

    class NoOpApiConfigPreProcessor implements IApiConfigPreProcessor {
        @Override
        public void process(String name, Api api) {
        }
    }
}

package io.github.harishb2k.easy.http.registry.helper;

import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;

public interface IClientBuilder {
    boolean accept(Server server, Api api);
    <T> T buildClient(Server server, Api api, Class<T> cls);
    void close();
}

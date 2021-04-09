package io.github.devlibx.easy.http.helper;

import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Server;

public interface IClientBuilder {

    /**
     * @return true if this builder can process it.
     */
    boolean accept(Server server, Api api);

    /**
     * @return build a client for server and api
     */
    <T> T buildClient(Server server, Api api, Class<T> cls);

    /**
     * Shutdown builder
     */
    void shutdown();
}

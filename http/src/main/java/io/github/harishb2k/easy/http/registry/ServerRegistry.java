package io.github.harishb2k.easy.http.registry;

import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.config.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerRegistry {
    private final Map<String, Server> serverMap;

    public ServerRegistry() {
        this.serverMap = new HashMap<>();
    }

    public void configure(Config config) {
        serverMap.putAll(config.getServers());
    }

    public Server get(String server) {
        return serverMap.get(server);
    }

    public Optional<Server> getOptional(String server) {
        return Optional.ofNullable(serverMap.get(server));
    }

    /**
     * Shutdown builder
     */
    public void shutdown() {
    }
}

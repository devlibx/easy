package io.github.harishb2k.easy.http.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    private Map<String, Server> servers;
    private Map<String, Api> apis;

    public void addServer(Server server) {
        if (servers == null) {
            servers = new HashMap<>();
        }
        servers.put(server.getName(), server);
    }

    public void addApi(Api api) {
        if (apis == null) {
            apis = new HashMap<>();
        }
        apis.put(api.getName(), api);
    }
}

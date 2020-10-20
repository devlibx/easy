package io.github.harishb2k.easy.database.mysql.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MySqlConfigs {
    private Map<String, MySqlConfig> configs;

    public void addConfig(MySqlConfig config) {
        addConfig("default", config);
    }

    public void addConfig(String name, MySqlConfig config) {
        if (configs == null) {
            configs = new HashMap<>();
        }
        configs.put(name, config);
    }
}

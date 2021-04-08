package io.github.devlibx.easy.database.mysql.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import static io.github.devlibx.easy.database.DatabaseConstant.DATASOURCE_DEFAULT;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MySqlConfigs {
    private Map<String, MySqlConfig> configs;

    public void addConfig(MySqlConfig config) {
        addConfig(DATASOURCE_DEFAULT, config);
    }

    public void addConfig(String name, MySqlConfig config) {
        if (configs == null) {
            configs = new HashMap<>();
        }
        configs.put(name, config);
    }
}

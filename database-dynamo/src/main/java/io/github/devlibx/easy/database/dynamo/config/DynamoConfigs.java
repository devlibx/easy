package io.github.devlibx.easy.database.dynamo.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import static io.github.devlibx.easy.database.DatabaseConstant.DATASOURCE_DEFAULT;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamoConfigs {
    private Map<String, DynamoConfig> configs;

    public void addConfig(DynamoConfig config) {
        addConfig(DATASOURCE_DEFAULT, config);
    }

    public void addConfig(String name, DynamoConfig config) {
        if (configs == null) {
            configs = new HashMap<>();
        }
        configs.put(name, config);
    }
}

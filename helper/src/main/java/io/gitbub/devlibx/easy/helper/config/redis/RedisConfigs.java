package io.gitbub.devlibx.easy.helper.config.redis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RedisConfigs {

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Map<String, RedisConfig> redisConfigs = new HashMap<>();

    @Builder.Default
    private StringObjectMap properties = new StringObjectMap();

    /**
     * Setup default
     */
    public void setupDefaults() {
        redisConfigs.forEach((name, redisConfig) -> {
            if (Strings.isNullOrEmpty(redisConfig.getName())) {
                redisConfig.setName(name);
            }
        });
    }

    /**
     * Get redis config by name or empty.
     *
     * @param name unique name for redis config
     * @return Optional.empty() if it is not defined or if not disable, otherwise redis config.
     */
    public Optional<RedisConfig> getRedisConfig(String name) {
        RedisConfig redisConfig = null;
        if (redisConfigs != null && redisConfigs.containsKey(name)) {
            redisConfig = redisConfigs.get(name);
            if (redisConfig.isEnabled()) {
                return Optional.of(redisConfig);
            }
        }
        return Optional.empty();
    }
}

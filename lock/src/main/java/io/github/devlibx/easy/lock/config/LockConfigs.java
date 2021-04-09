package io.github.devlibx.easy.lock.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LockConfigs {
    private Map<String, LockConfig> lockConfigs = new ConcurrentHashMap<>();

    /**
     * Add a lock config to
     */
    public void addLockConfig(LockConfig lockConfig) {
        if (lockConfigs == null) {
            lockConfigs = new ConcurrentHashMap<>();
        }
        lockConfigs.put(lockConfig.getName(), lockConfig);
    }
}

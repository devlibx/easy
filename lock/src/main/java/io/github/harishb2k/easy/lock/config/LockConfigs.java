package io.github.harishb2k.easy.lock.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LockConfigs {
    private Map<String, LockConfig> lockConfigs;

    /**
     * Add a lock config to
     */
    public void addLockConfig(LockConfig lockConfig) {
        if (lockConfigs == null) {
            lockConfigs = new HashMap<>();
        }
        lockConfigs.put(lockConfig.getName(), lockConfig);
    }
}

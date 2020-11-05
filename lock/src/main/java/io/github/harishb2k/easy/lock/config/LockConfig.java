package io.github.harishb2k.easy.lock.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LockConfig {
    private String name;
    private int timeoutInMs = 5 * 1000;
    private String type;
}

package io.github.harishb2k.easy.lock.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LockConfig {
    private String name;
    private int timeoutInSec = 5;
    private String type;
}

package io.gitbub.harishb2k.easy.helper.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricsConfig {
    private String prefix = "";
    private boolean enabled = false;
}

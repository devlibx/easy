package io.gitbub.devlibx.easy.helper.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetricsConfig {
    private String prefix = "";
    private String env;
    private String host;
    private int port;
    private int pushInterval;
    private String serviceName;
    private boolean enabled = false;
}

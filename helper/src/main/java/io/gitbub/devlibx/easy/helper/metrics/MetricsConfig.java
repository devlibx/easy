package io.gitbub.devlibx.easy.helper.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
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

    /**
     * buffer-size - default = 1000000 (Only for StatsD). It works on UDP, and we do not want to block on send. So we
     * have a buffer queue of fixed size. If we are not able to send messages fast enough, it will be full and we will
     * drop stats.
     * <p>
     * user-timgroup-statsd-client - default - true
     * if you do not want to use " com.timgroup.statsd" package for statsD clients then set it false
     */
    @Builder.Default
    private StringObjectMap properties = new StringObjectMap();
}

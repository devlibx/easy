package io.gitbub.devlibx.easy.helper.config.redis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RedisConfig {

    private String name;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String host = "localhost";

    @Builder.Default
    private int port = 6379;

    @Builder.Default
    private StringObjectMap properties = new StringObjectMap();

    @Builder.Default
    private String strategy = "single-server";

    @Builder.Default
    private int timeout = 3000;

    @Builder.Default
    @JsonProperty("connect_timeout")
    private int connectTimeout = 10000;

    @Builder.Default
    @JsonProperty("idle_connection_timeout")
    private int idleConnectionTimeout = 10000;

    @Builder.Default
    @JsonProperty("ping_connection_interval")
    private int pingConnectionInterval = 30000;
}

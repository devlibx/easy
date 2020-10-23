package io.github.harishb2k.easy.http.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Server {

    /**
     * Name for this server
     */
    private String name;

    /**
     * Host URL
     */
    private String host;

    private boolean isHttps = false;

    /**
     * Port of this service. Default = 80
     */
    private int port = 80;

    /**
     * Set TTL to polled http connection. If -1 then no TTL. Default is 60 Sec;
     */
    private int pollingConnectionTtlInMs = 60 * 1000;

    private int idleConnectionTimeoutInSec = 60;
}

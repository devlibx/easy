package io.github.devlibx.easy.http.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Server implements Serializable {

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

    /**
     * A http request needs to get a connection from pool. This timeout specifies how long this
     * connection request should wait.
     * <p>
     * Default = 100ms
     * <p>
     * Why we get this type of timeout -> if we have small connection pool and all connections are
     * busy then we will get this timeout.
     */
    private int connectionRequestTimeout = 100;

    /**
     * A http request needs to connect to a server first. This timeout specifies how long this
     * connection creation should take. Beyond this time it will fail with timeout.
     * <p>
     * Default = 100ms
     * <p>
     * Why we get this type of timeout -> when we connect to server, it may be busy or server may be down.
     */
    private int connectTimeout = 100;

    private StringObjectMap headers;

    public String getUrl() {
        StringBuilder sb = new StringBuilder();
        if (isHttps) {
            sb.append("https://");
        } else {
            sb.append("http://");
        }
        sb.append(host);
        sb.append(":");
        sb.append(port);
        return sb.toString();
    }
}

package io.github.harishb2k.easy.http.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Api {

    /**
     * Name of the server to be used for this request
     */
    private String server;

    private String name;

    private String type = "HTTP";

    /**
     * API path
     */
    private String path;

    /**
     * Max no of concurrent request for this APIs
     */
    private int concurrency = 2;

    /**
     * If server took > timeout to complete a request then this timeout will occure.
     * <p>
     * 1 sec is default.
     */
    private int timeout = 1000;

    /**
     * We add this extra time to overall request.
     */
    private float timeoutDeltaFactor = 0.1f;

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

}

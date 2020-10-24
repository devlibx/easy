package io.github.harishb2k.easy.http.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Api {

    /**
     * Default set of acceptable codes
     */
    public static final List<Integer> DEFAULT_ACCEPTABLE_CODES = Collections.unmodifiableList(Arrays.asList(200, 201));

    /**
     * Name of this API
     */
    private String name;

    /**
     * Fallback API to call if this API fails
     */
    private String fallbackApiName;

    /**
     * Use async HTTP client i.e. use async-io
     * <p>
     * e.g. a netty based HTTP client to make call to server
     */
    private boolean async;

    /**
     * We will warm-up all the http connection pool and threads at the time of boot-up. If noWarmUp=true then this
     * bootstrap process will not be done.
     * <p>
     * default = false i.e. warm-up connections and thread at bootup
     */
    private boolean noWarmUp;

    /**
     * Name of the server to be used for this request
     */
    private String server;

    /**
     * HTTP method to be used for this request
     */
    private String method = "GET";

    /**
     * Type of this API (valid type = HTTP | HTTPS )
     */
    private String type = "HTTP";

    /**
     * What status codes are considered as acceptable. If we get http status in response which
     * is one of these then that call will not be considered as failure.
     */
    private List<Integer> acceptableCodes = DEFAULT_ACCEPTABLE_CODES;

    /**
     * API path
     */
    private String path;

    /**
     * Max no of concurrent request for this APIs
     */
    private int concurrency = 2;

    /**
     * concurrency is the no of parallel request this API can handle. queueSize is the additional queue where we park
     * a request if we are already processing "N=concurrency" requests.
     * <p>
     * Once we process the running requests, we pick the request from this queue.
     * <p>
     * e.g. concurrency=2 and queueSize=100
     * <p>
     * This means that we can take burst of 102 requests.
     */
    private int queueSize = 100;

    /**
     * If server took > timeout to complete a request then this timeout will occure.
     * <p>
     * 1 sec is default.
     */
    private int timeout = 1000;

    /**
     * We add this extra time to overall request. This is to account for time consumed in other activities
     * e.g. connection creation, thread allocating if required etc.
     *
     * <p>
     * e.g. If your timeout is 1000 and this value is 0.1 then we will set socket timeout = (1000 + 1000 * 0.1 = 1100)
     */
    private float timeoutDeltaFactor = 0.0f;
}

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

    public static final List<Integer> DEFAULT_ACCEPTABLE_CODES = Collections.unmodifiableList(Arrays.asList(200, 201));

    /**
     * Name of this API
     */
    private String name;

    /**
     * Fallback API to call if this API fails
     */
    private String fallbackApiName;

    private boolean async;

    /**
     * Name of the server to be used for this request
     */
    private String server;

    private String method = "GET";

    /**
     * Type of this API (valid type = HTTP | HTTPS )
     */
    private String type = "HTTP";

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

package io.github.devlibx.easy.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.http.util.Call.IResponseBuilderFunc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestObject {
    private String server;
    private String api;
    private String method;
    private Map<String, Object> headers;
    private Map<String, Object> pathParam;
    private MultivaluedMap<String, Object> queryParam;
    private byte[] body;
    private IResponseBuilderFunc<?> responseBuilder;

    public void preProcessHeaders(StringObjectMap apiHeaders) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (!headers.containsKey("Content-Type") && !headers.containsKey("content-type")) {
            headers.put("Content-Type", "application/json");
        }
        if (apiHeaders != null) {
            headers.putAll(apiHeaders);
        }
    }
}

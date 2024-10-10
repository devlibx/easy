package io.github.devlibx.easy.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseObject {
    private byte[] body;
    private int statusCode;

    @JsonIgnore
    private boolean success;

    @JsonIgnore
    private Throwable exception;

    @JsonIgnore
    private boolean errorWithAcceptableErrorCode;

    public Map<String, Object> convertAsMap() {
        if (body == null) return null;
        return JsonUtils.convertAsMap(new String(body));
    }

    public String getBodyAsString() {
        if (body == null) return null;
        return new String(body);
    }
}

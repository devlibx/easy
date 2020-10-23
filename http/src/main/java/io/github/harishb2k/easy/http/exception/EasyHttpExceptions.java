package io.github.harishb2k.easy.http.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.harishb2k.easy.http.ResponseObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.ws.rs.core.Response;
import java.util.Map;

public interface EasyHttpExceptions {

    @Getter
    @AllArgsConstructor
    @Builder
    class EasyHttpRequestException extends RuntimeException {
        private final int statusCode;
        private final byte[] body;
        private final ResponseObject response;

        public EasyHttpRequestException(Throwable throwable) {
            super(throwable);
            statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            body = null;
            response = null;
        }

        public EasyHttpRequestException(ResponseObject response) {
            super(String.format("statusCode=%d", response.getStatusCode()));
            this.statusCode = response.getStatusCode();
            this.body = response.getBody();
            this.response = response;
        }

        @JsonIgnore
        public String getResponseAsString() {
            return response != null ? response.getBodyAsString() : null;
        }

        @JsonIgnore
        public Map<String, Object> getResponseAsMap() {
            return response != null ? response.convertAsMap() : null;
        }
    }
}

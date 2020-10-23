package io.github.harishb2k.easy.http.exception;

import io.github.harishb2k.easy.http.ResponseObject;
import lombok.AllArgsConstructor;
import lombok.Builder;

public interface EasyHttpExceptions {

    @AllArgsConstructor
    @Builder
    class EasyHttpRequestException extends RuntimeException {
        private int statusCode;
        private byte[] body;
        private ResponseObject response;

        public EasyHttpRequestException(ResponseObject response) {
            this.statusCode = response.getStatusCode();
            this.body = response.getBody();
            response = this.response;
        }
    }
}

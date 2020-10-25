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

        public EasyHttpRequestException(String message, ResponseObject response) {
            super(message);
            this.statusCode = response.getStatusCode();
            this.body = response.getBody();
            this.response = response;
        }

        public EasyHttpRequestException(ResponseObject response) {
            this(String.format("statusCode=%d", response.getStatusCode()), response);
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

    class Easy4xxException extends EasyHttpRequestException {
        public Easy4xxException(ResponseObject response) {
            super(response);
        }
    }

    class Easy5xxException extends EasyHttpRequestException {
        public Easy5xxException(ResponseObject response) {
            super(response);
        }
    }

    class EasyGatewayTimeoutException extends Easy5xxException {
        public EasyGatewayTimeoutException(ResponseObject response) {
            super(response);
        }
    }

    class EasyServiceUnavailableException extends Easy5xxException {
        public EasyServiceUnavailableException(ResponseObject response) {
            super(response);
        }
    }

    class EasyBadGatewayException extends Easy5xxException {
        public EasyBadGatewayException(ResponseObject response) {
            super(response);
        }
    }

    class EasyNotImplementedException extends Easy5xxException {
        public EasyNotImplementedException(ResponseObject response) {
            super(response);
        }
    }

    class EasyInternalServerErrorException extends Easy5xxException {
        public EasyInternalServerErrorException(ResponseObject response) {
            super(response);
        }
    }

    class EasyTooManyRequestsException extends Easy4xxException {
        public EasyTooManyRequestsException(ResponseObject response) {
            super(response);
        }
    }

    class EasyBadRequestException extends Easy4xxException {
        public EasyBadRequestException(ResponseObject response) {
            super(response);
        }
    }

    class EasyGoneException extends Easy4xxException {
        public EasyGoneException(ResponseObject response) {
            super(response);
        }
    }


    class EasyConflictRequestException extends Easy4xxException {
        public EasyConflictRequestException(ResponseObject response) {
            super(response);
        }
    }

    class EasyNotAcceptableException extends Easy4xxException {
        public EasyNotAcceptableException(ResponseObject response) {
            super(response);
        }
    }

    class EasyMethodNotAllowedException extends Easy4xxException {
        public EasyMethodNotAllowedException(ResponseObject response) {
            super(response);
        }
    }

    class EasyNotFoundException extends Easy4xxException {
        public EasyNotFoundException(ResponseObject response) {
            super(response);
        }
    }

    class EasyUnauthorizedRequestException extends Easy4xxException {
        public EasyUnauthorizedRequestException(ResponseObject response) {
            super(response);
        }
    }

    class EasyRequestTimeOutException extends Easy4xxException {
        public EasyRequestTimeOutException(ResponseObject response) {
            super(response);
        }
    }


    class EasyResilienceException extends EasyHttpRequestException {
        public EasyResilienceException(Throwable throwable) {
            super(throwable);
        }
    }

    class EasyResilienceRequestTimeoutException extends EasyResilienceException {
        public EasyResilienceRequestTimeoutException(Throwable throwable) {
            super(throwable);
        }
    }

    class EasyResilienceOverflowException extends EasyResilienceException {
        public EasyResilienceOverflowException(Throwable throwable) {
            super(throwable);
        }
    }

    class EasyResilienceCircuitOpenException extends EasyResilienceException {
        public EasyResilienceCircuitOpenException(Throwable throwable) {
            super(throwable);
        }
    }
}

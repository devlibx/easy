package io.github.harishb2k.easy.http.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.resilience.exception.CircuitOpenException;
import io.github.harishb2k.easy.resilience.exception.OverflowException;
import io.github.harishb2k.easy.resilience.exception.RequestTimeoutException;
import io.github.harishb2k.easy.resilience.exception.ResilienceException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

public class EasyHttpExceptions {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class EasyHttpRequestException extends RuntimeException {
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

    public static class Easy4xxException extends EasyHttpRequestException {
        public Easy4xxException(ResponseObject response) {
            super(response);
        }
    }

    public static class Easy5xxException extends EasyHttpRequestException {
        public Easy5xxException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyGatewayTimeoutException extends Easy5xxException {
        public EasyGatewayTimeoutException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyServiceUnavailableException extends Easy5xxException {
        public EasyServiceUnavailableException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyBadGatewayException extends Easy5xxException {
        public EasyBadGatewayException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyNotImplementedException extends Easy5xxException {
        public EasyNotImplementedException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyInternalServerErrorException extends Easy5xxException {
        public EasyInternalServerErrorException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyTooManyRequestsException extends Easy4xxException {
        public EasyTooManyRequestsException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyBadRequestException extends Easy4xxException {
        public EasyBadRequestException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyGoneException extends Easy4xxException {
        public EasyGoneException(ResponseObject response) {
            super(response);
        }
    }


    public static class EasyConflictRequestException extends Easy4xxException {
        public EasyConflictRequestException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyNotAcceptableException extends Easy4xxException {
        public EasyNotAcceptableException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyMethodNotAllowedException extends Easy4xxException {
        public EasyMethodNotAllowedException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyNotFoundException extends Easy4xxException {
        public EasyNotFoundException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyUnauthorizedRequestException extends Easy4xxException {
        public EasyUnauthorizedRequestException(ResponseObject response) {
            super(response);
        }
    }

    public static class EasyRequestTimeOutException extends Easy4xxException {
        public EasyRequestTimeOutException(ResponseObject response) {
            super(response);
        }
    }


    public static class EasyResilienceException extends EasyHttpRequestException {
        public EasyResilienceException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class EasyResilienceRequestTimeoutException extends EasyResilienceException {
        public EasyResilienceRequestTimeoutException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class EasyResilienceOverflowException extends EasyResilienceException {
        public EasyResilienceOverflowException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class EasyResilienceCircuitOpenException extends EasyResilienceException {
        public EasyResilienceCircuitOpenException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Helper to convert ResilienceException to Easy Exceptions
     */
    public static Optional<EasyResilienceException> easyEasyResilienceException(Throwable e) {
        if (e instanceof RequestTimeoutException) {
            return Optional.of(new EasyResilienceRequestTimeoutException(e));
        } else if (e instanceof OverflowException) {
            return Optional.of(new EasyResilienceOverflowException(e));
        } else if (e instanceof CircuitOpenException) {
            return Optional.of(new EasyResilienceCircuitOpenException(e));
        } else if (e instanceof ResilienceException) {
            return Optional.of(new EasyResilienceException(e));
        } else {
            return Optional.empty();
        }
    }
}

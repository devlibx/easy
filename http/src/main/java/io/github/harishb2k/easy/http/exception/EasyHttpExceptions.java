package io.github.harishb2k.easy.http.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.resilience.exception.CircuitOpenException;
import io.github.harishb2k.easy.resilience.exception.OverflowException;
import io.github.harishb2k.easy.resilience.exception.RequestTimeoutException;
import io.github.harishb2k.easy.resilience.exception.ResilienceException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.ws.rs.core.Response;
import java.net.SocketTimeoutException;
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
            super(message, response.getException() != null ? response.getException() : null);
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

    /**
     * This exception is thrown when a socket timeout occurs
     */
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

    /**
     * This error is thrown when request is timed out due to you timeout specified in "api.timeout" property.
     * <p>
     * Note - socket timeout is different which is captured by {@link EasyRequestTimeOutException}.
     */
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

    public static EasyHttpRequestException convert(int statusCode, Throwable throwable, ResponseObject responseObject) {

        // First check if this is a Resilience exception
        EasyHttpRequestException exception = easyEasyResilienceException(throwable).orElse(null);
        if (exception != null) {
            return exception;
        }

        // Check if this is a timeout issues
        if (throwable instanceof ReadTimeoutException || throwable instanceof SocketTimeoutException) {
            return new EasyRequestTimeOutException(responseObject);
        } else if (throwable.getCause() instanceof ReadTimeoutException) {
            return new EasyRequestTimeOutException(responseObject);
        }

        // If we got WebClientResponseException use status code from WebClientResponseException
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            statusCode = ex.getRawStatusCode();
        }
        
        // Try to build exceptions from status code
        if (statusCode == Response.Status.GATEWAY_TIMEOUT.getStatusCode()) {
            exception = new EasyGatewayTimeoutException(responseObject);
        } else if (statusCode == Response.Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            exception = new EasyServiceUnavailableException(responseObject);
        } else if (statusCode == Response.Status.BAD_GATEWAY.getStatusCode()) {
            exception = new EasyBadGatewayException(responseObject);
        } else if (statusCode == Response.Status.NOT_IMPLEMENTED.getStatusCode()) {
            exception = new EasyNotImplementedException(responseObject);
        } else if (statusCode == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            exception = new EasyInternalServerErrorException(responseObject);
        } else if (statusCode == 429 /* TooManyRequests */) {
            exception = new EasyTooManyRequestsException(responseObject);
        } else if (statusCode == 422 /* TooManyRequests */) {
            exception = new EasyBadRequestException(responseObject);
        } else if (statusCode == Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()) {
            exception = new EasyBadRequestException(responseObject);
        } else if (statusCode == Response.Status.GONE.getStatusCode()) {
            exception = new EasyGoneException(responseObject);
        } else if (statusCode == Response.Status.CONFLICT.getStatusCode()) {
            exception = new EasyConflictRequestException(responseObject);
        } else if (statusCode == Response.Status.NOT_ACCEPTABLE.getStatusCode()) {
            exception = new EasyNotAcceptableException(responseObject);
        } else if (statusCode == Response.Status.METHOD_NOT_ALLOWED.getStatusCode()) {
            exception = new EasyMethodNotAllowedException(responseObject);
        } else if (statusCode == Response.Status.NOT_FOUND.getStatusCode()) {
            exception = new EasyNotFoundException(responseObject);
        } else if (statusCode == Response.Status.FORBIDDEN.getStatusCode()) {
            exception = new EasyUnauthorizedRequestException(responseObject);
        } else if (statusCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
            exception = new EasyUnauthorizedRequestException(responseObject);
        } else if (statusCode == Response.Status.BAD_REQUEST.getStatusCode()) {
            exception = new EasyBadRequestException(responseObject);
        } else {
            exception = new EasyHttpRequestException(responseObject);
        }

        return exception;
    }
}

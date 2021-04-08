package io.github.devlibx.easy.resilience.exception;

public class RequestTimeoutException extends ResilienceException {
    public RequestTimeoutException(String message, Throwable e) {
        super("Request timeout - " + message, e);
    }
}

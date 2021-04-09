package io.github.devlibx.easy.resilience.exception;

public class ResilienceException extends RuntimeException {
    public ResilienceException(String message, Throwable e) {
        super(message, e);
    }
}

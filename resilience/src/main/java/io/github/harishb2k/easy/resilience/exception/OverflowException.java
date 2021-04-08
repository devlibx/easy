package io.github.devlibx.easy.resilience.exception;

public class OverflowException extends ResilienceException {
    public OverflowException(String message, Throwable e) {
        super("Request overflow - " + message, e);
    }
}

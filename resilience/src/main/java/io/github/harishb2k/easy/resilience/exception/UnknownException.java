package io.github.harishb2k.easy.resilience.exception;

public class UnknownException extends ResilienceException {
    public UnknownException(String message, Throwable e) {
        super("Unknown error - " + message, e);
    }
}

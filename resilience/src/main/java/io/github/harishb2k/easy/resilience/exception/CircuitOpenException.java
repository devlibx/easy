package io.github.harishb2k.easy.resilience.exception;

public class CircuitOpenException extends ResilienceException {
    public CircuitOpenException(String message, Throwable e) {
        super("Circuit is open - " + message, e);
    }
}

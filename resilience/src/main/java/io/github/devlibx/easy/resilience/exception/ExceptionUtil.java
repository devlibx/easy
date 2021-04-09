package io.github.devlibx.easy.resilience.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExceptionUtil {

    private static RuntimeException unwrap(Throwable e) {
        if (e instanceof TimeoutException) {
            return new RequestTimeoutException(e.getMessage(), e);
        } else if (e instanceof CallNotPermittedException) {
            return new CircuitOpenException(e.getMessage(), e);
        } else if (e instanceof BulkheadFullException) {
            return new OverflowException(e.getMessage(), e);
        }
        return null;
    }

    public static RuntimeException unwrapResilience4jException(Throwable e) {
        RuntimeException ex = unwrap(e);
        if (ex == null) {
            ex = unwrap(e.getCause());
        }
        if (ex == null && e instanceof RuntimeException) {
            ex = (RuntimeException) e;
        }
        return ex == null ? new UnknownException("Unknown exception", e) : ex;
    }

    public static RuntimeException unwrapResilience4jExecutionException(ExecutionException e) {
        if (e.getCause() instanceof BulkheadFullException) {
            BulkheadFullException exception = (BulkheadFullException) e.getCause();
            return new OverflowException(exception.getMessage(), e);
        } else if (e.getCause() instanceof CallNotPermittedException) {
            CallNotPermittedException exception = (CallNotPermittedException) e.getCause();
            return new CircuitOpenException(exception.getMessage(), e);
        } else if (e.getCause() instanceof TimeoutException) {
            return new RequestTimeoutException(e.getMessage(), e);
        } else {
            return new UnknownException(e.getMessage(), e.getCause());
        }
    }

    public static RuntimeException processException(Exception e) {
        if (e instanceof UnknownException) {
            return (RuntimeException) e.getCause();
        } else if (e.getCause() instanceof ExecutionException) {
            return processException((Exception) e.getCause());
        } else {
            return new RuntimeException(e);
        }
    }

    public static RuntimeException processException(ExecutionException e) {
        if (e.getCause() instanceof BulkheadFullException) {
            BulkheadFullException exception = (BulkheadFullException) e.getCause();
            return new OverflowException(exception.getMessage(), e);
        } else if (e.getCause() instanceof CallNotPermittedException) {
            CallNotPermittedException exception = (CallNotPermittedException) e.getCause();
            return new CircuitOpenException(exception.getMessage(), e);
        } else if (e.getCause() instanceof TimeoutException) {
            return new RequestTimeoutException(e.getMessage(), e);
        } else {
            return new UnknownException(e.getMessage(), e.getCause());
        }
    }
}

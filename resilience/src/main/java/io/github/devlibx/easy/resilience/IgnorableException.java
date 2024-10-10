package io.github.devlibx.easy.resilience;

/** If this exception can be ignored then Hystrix circuit will not open */
public interface IgnorableException {
    boolean canIgnoreException();
}

package io.github.devlibx.easy.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String group() default "default";

    String name() default "default";

    Class<? extends IDistributedLockIdResolver> lockIdResolver();
}

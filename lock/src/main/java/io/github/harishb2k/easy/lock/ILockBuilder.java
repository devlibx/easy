package io.github.devlibx.easy.lock;

import io.github.devlibx.easy.lock.config.LockConfig;

public interface ILockBuilder {
    IDistributedLock create(LockConfig lockConfig);
}

package io.github.harishb2k.easy.lock;

import io.github.harishb2k.easy.lock.config.LockConfig;

public interface ILockBuilder {
    IDistributedLock create(LockConfig lockConfig);
}

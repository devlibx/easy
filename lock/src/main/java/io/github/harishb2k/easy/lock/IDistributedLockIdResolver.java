package io.github.harishb2k.easy.lock;

import io.github.harishb2k.easy.lock.IDistributedLock.LockRequest;

public interface IDistributedLockIdResolver {
    LockRequest createLockRequest(Object[] arguments);
}

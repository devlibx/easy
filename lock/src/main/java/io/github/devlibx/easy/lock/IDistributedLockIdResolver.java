package io.github.devlibx.easy.lock;

import io.github.devlibx.easy.lock.IDistributedLock.LockRequest;
import org.aopalliance.intercept.MethodInvocation;

public interface IDistributedLockIdResolver {
    LockRequest createLockRequest(MethodInvocation invocation, Object[] arguments);
}

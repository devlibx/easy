package io.github.harishb2k.easy.lock;

import io.github.harishb2k.easy.lock.IDistributedLock.LockRequest;
import org.aopalliance.intercept.MethodInvocation;

public interface IDistributedLockIdResolver {
    LockRequest createLockRequest(MethodInvocation invocation, Object[] arguments);
}

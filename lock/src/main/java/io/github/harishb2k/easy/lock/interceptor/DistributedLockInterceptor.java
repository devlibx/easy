package io.github.harishb2k.easy.lock.interceptor;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.lock.DistributedLock;
import io.github.harishb2k.easy.lock.IDistributedLock;
import io.github.harishb2k.easy.lock.IDistributedLock.LockRequest;
import io.github.harishb2k.easy.lock.IDistributedLockIdResolver;
import io.github.harishb2k.easy.lock.IDistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.locks.Lock;

@Slf4j
public class DistributedLockInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        DistributedLock distributedLock = invocation.getMethod().getAnnotation(DistributedLock.class);
        if (distributedLock == null) {
            log.trace("Execute method without lock - method is not marked with DistributedLock");
            return invocation.proceed();
        }

        // Get the lock
        IDistributedLockService distributedLockService = ApplicationContext.getInstance(IDistributedLockService.class);
        IDistributedLock lock = distributedLockService.getLock(distributedLock.name());
        if (lock == null) {
            throw new RuntimeException("Field to find lock with name=" + distributedLock.name() + ": Make suer you added a LockConfig with this name");
        }

        // Find a lock ID resolver
        IDistributedLockIdResolver distributedLockIdResolver = ApplicationContext.getInstance(distributedLock.lockIdResolver());
        LockRequest lockRequest = distributedLockIdResolver.createLockRequest(invocation.getArguments());
        log.debug("Lock Request = {}, distributedLockAnnotation={}", lockRequest, distributedLock);

        // Acquire lock for given id
        Lock underlyingLock = null;
        try {
            underlyingLock = lock.achieveLock(lockRequest);
            return invocation.proceed();
        } finally {
            if (underlyingLock != null) {
                lock.releaseLock(underlyingLock, lockRequest);
            }
        }
    }
}

package io.github.devlibx.easy.lock.interceptor;

import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.github.devlibx.easy.lock.DistributedLock;
import io.github.devlibx.easy.lock.IDistributedLock;
import io.github.devlibx.easy.lock.IDistributedLock.LockRequest;
import io.github.devlibx.easy.lock.IDistributedLockIdResolver;
import io.github.devlibx.easy.lock.IDistributedLockService;
import io.github.devlibx.easy.lock.IDistributedLockService.ExistingLockWithNoOp;
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
        IDistributedLock lock = null;
        try {
            IDistributedLockService distributedLockService = ApplicationContext.getInstance(IDistributedLockService.class);
            lock = distributedLockService.getLock(distributedLock.name());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                e.printStackTrace();
            }
            throw new RuntimeException("Field to find lock with name=" + distributedLock.name() + ": Make suer you added a LockConfig with this name", e);
        }

        // Make sure lock is not null
        if (lock == null) {
            throw new RuntimeException("Field to find lock with name=" + distributedLock.name() + ": Make suer you added a LockConfig with this name");
        }

        // Find a lock ID resolver
        IDistributedLockIdResolver distributedLockIdResolver = ApplicationContext.getInstance(distributedLock.lockIdResolver());
        LockRequest lockRequest = distributedLockIdResolver.createLockRequest(invocation, invocation.getArguments());
        log.trace("Lock Request = {}, distributedLockAnnotation={}", lockRequest, distributedLock);

        // Set lock name and group if it is not given
        if (Strings.isNullOrEmpty(lockRequest.getName())) {
            lockRequest.setName(distributedLock.name());
        }
        if (Strings.isNullOrEmpty(lockRequest.getLockGroup())) {
            lockRequest.setLockGroup(distributedLock.group());
        }

        // Acquire lock for given id
        Lock underlyingLock = null;
        try {
            log.trace("Trying to acquire lock - request={}", lockRequest);
            underlyingLock = lock.achieveLock(lockRequest);
            if (underlyingLock instanceof ExistingLockWithNoOp) {
                log.info("acquired a existing lock: (this is a no-op lock) - lock={}", underlyingLock);
            }
            log.debug("Lock acquired - request={}", lockRequest);
            return invocation.proceed();
        } finally {
            if (underlyingLock != null) {
                lock.releaseLock(underlyingLock, lockRequest);
                log.debug("Lock released - request={}", lockRequest);
            }
        }
    }
}

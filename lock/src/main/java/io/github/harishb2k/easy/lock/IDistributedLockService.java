package io.github.harishb2k.easy.lock;

import com.google.inject.ImplementedBy;
import io.github.harishb2k.easy.lock.IDistributedLock.LockRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@ImplementedBy(IDistributedLockService.NoOpDistributedLockService.class)
public interface IDistributedLockService {
    String NO_OP_LOCK_NAME = "__no_op_lock_name__";

    /**
     * Initialize locks
     */
    void initialize();

    /**
     * Release all locks
     */
    void shutdown();

    IDistributedLock getLock(String name);

    /**
     * No-Op implementation for lock service
     */
    class NoOpDistributedLockService implements IDistributedLockService {
        @Override
        public void initialize() {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public IDistributedLock getLock(String name) {
            return new NoOpDistributedLock();
        }
    }

    /**
     * No-Op implementation for lock
     */
    @Slf4j
    class NoOpDistributedLock implements IDistributedLock {
        @Override
        public Lock achieveLock(LockRequest request) {
            log.warn("NoOpDistributedLock - (no lock is taken - it is no-op implementation): request={}", request);
            return new NoOpLock();
        }

        @Override
        public void releaseLock(Lock lock, LockRequest lockRequest) {
        }
    }

    /**
     * No-Op implementation for lock
     */
    class NoOpLock implements Lock {

        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    /**
     * A no-op implementation which holds existing lock
     */
    @Data
    class ExistingLockWithNoOp extends NoOpLock {
        private final Lock existingLock;

        public ExistingLockWithNoOp(Lock existingLock) {
            this.existingLock = existingLock;
        }
    }

    /**
     * No op lock id resolver
     */
    class NoOpDistributedLockIdResolver implements IDistributedLockIdResolver {
        @Override
        public IDistributedLock.LockRequest createLockRequest(MethodInvocation invocation, Object[] arguments) {
            return LockRequest.builder()
                    .lockId(UUID.randomUUID().toString())
                    .build();
        }
    }
}

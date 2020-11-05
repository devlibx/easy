package io.github.harishb2k.easy.lock;

import io.github.harishb2k.easy.lock.config.LockConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.locks.Lock;

public interface IDistributedLock {

    /**
     * Setup a lock
     */
    default void setup(LockConfig lockConfig) {
    }

    /**
     * Teardown a lock and release andy resources used
     */
    default void tearDown() {
    }

    /**
     * Take a lock
     *
     * @return a lock object once lock is taken
     */
    Lock achieveLock(LockRequest request);

    /**
     * Release a lock
     */
    void releaseLock(Lock lock, LockRequest lockRequest);

    /**
     * Request to take a lock
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class LockRequest {
        private String name;
        private String lockId;
        private String lockGroup;

        public String getUniqueLockIdForLocking() {
            return lockId;
        }
    }
}

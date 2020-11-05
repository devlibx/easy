package io.github.harishb2k.easy.lock;

import com.google.inject.ImplementedBy;

public interface IDistributedLockService {

    /**
     * Initialize locks
     */
    void initialize();

    /**
     * Release all locks
     */
    void shutdown();

    IDistributedLock getLock(String name);
}

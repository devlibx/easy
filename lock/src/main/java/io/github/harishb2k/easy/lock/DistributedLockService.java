package io.github.harishb2k.easy.lock;

import io.gitbub.harishb2k.easy.helper.Safe;
import io.github.harishb2k.easy.lock.config.LockConfigs;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DistributedLockService implements IDistributedLockService {
    private final Map<String, IDistributedLock> lockMap;
    private final LockConfigs lockConfigs;
    private final Map<String, ILockBuilder> lockBuilders;
    private boolean initialized = false;
    private final Object INITIALIZE_LOCK = new Object();

    @Inject
    public DistributedLockService(LockConfigs lockConfigs, Map<String, ILockBuilder> lockBuilders) {
        this.lockBuilders = lockBuilders;
        this.lockMap = new HashMap<>();
        this.lockConfigs = lockConfigs;
    }

    @Override
    public void initialize() {
        synchronized (INITIALIZE_LOCK) {
            lockConfigs.getLockConfigs().forEach((lock, lockConfig) -> {
                lockConfig.setName(lock);
                IDistributedLock distributedLock = lockBuilders.get(lockConfig.getType()).create(lockConfig);
                distributedLock.setup(lockConfig);
                lockMap.put(lock, distributedLock);
            });
            initialized = true;
        }
    }

    @Override
    public void shutdown() {
        Safe.safe(() -> {
            lockMap.forEach((lock, distributedLock) -> {
                Safe.safe(distributedLock::tearDown, "failed to teardown a lock: lock=" + lock);
            });
        });
    }

    @Override
    public IDistributedLock getLock(String name) {

        // Initialize it before we use it
        if (!initialized) {
            synchronized (INITIALIZE_LOCK) {
                if (!initialized) {
                    initialize();
                }
            }
        }

        if (Objects.equals(NO_OP_LOCK_NAME, name)) {
            return new NoOpDistributedLock();
        }

        return lockMap.get(name);
    }
}

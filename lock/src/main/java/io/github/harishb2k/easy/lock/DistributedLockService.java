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

    @Inject
    public DistributedLockService(LockConfigs lockConfigs, Map<String, ILockBuilder> lockBuilders) {
        this.lockBuilders = lockBuilders;
        this.lockMap = new HashMap<>();
        this.lockConfigs = lockConfigs;
    }

    @Override
    public void initialize() {
        lockConfigs.getLockConfigs().forEach((lock, lockConfig) -> {
            lockConfig.setName(lock);
            IDistributedLock distributedLock = lockBuilders.get(lockConfig.getType()).create(lockConfig);
            distributedLock.setup(lockConfig);
            lockMap.put(lock, distributedLock);
        });
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
        if (Objects.equals(NO_OP_LOCK_NAME, name)) {
            return new NoOpDistributedLock();
        }
        return lockMap.get(name);
    }
}

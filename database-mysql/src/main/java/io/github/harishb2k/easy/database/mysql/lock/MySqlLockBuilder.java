package io.github.harishb2k.easy.database.mysql.lock;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.lock.IDistributedLock;
import io.github.harishb2k.easy.lock.ILockBuilder;
import io.github.harishb2k.easy.lock.config.LockConfig;

public class MySqlLockBuilder implements ILockBuilder {
    @Override
    public IDistributedLock create(LockConfig lockConfig) {
        return ApplicationContext.getInstance(MySqlDistributedLock.class);
    }
}

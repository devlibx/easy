package io.github.devlibx.easy.database.mysql.lock;

import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.github.devlibx.easy.lock.IDistributedLock;
import io.github.devlibx.easy.lock.ILockBuilder;
import io.github.devlibx.easy.lock.config.LockConfig;

public class MySqlLockBuilder implements ILockBuilder {
    @Override
    public IDistributedLock create(LockConfig lockConfig) {
        return ApplicationContext.getInstance(MySqlDistributedLockV2.class);
    }
}

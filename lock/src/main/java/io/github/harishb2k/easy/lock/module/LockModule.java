package io.github.devlibx.easy.lock.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import io.github.devlibx.easy.lock.DistributedLock;
import io.github.devlibx.easy.lock.DistributedLockService;
import io.github.devlibx.easy.lock.IDistributedLockService;
import io.github.devlibx.easy.lock.interceptor.DistributedLockInterceptor;

public abstract class LockModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IDistributedLockService.class).to(DistributedLockService.class).in(Scopes.SINGLETON);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(DistributedLock.class), distributedLockInterceptor());
    }

    /***
     * @return DistributedLockInterceptor which will handle method annotated with @{@link DistributedLock}
     */
    protected DistributedLockInterceptor distributedLockInterceptor() {
        return new DistributedLockInterceptor();
    }
}

package io.github.harishb2k.easy.lock.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import io.github.harishb2k.easy.lock.DistributedLock;
import io.github.harishb2k.easy.lock.DistributedLockService;
import io.github.harishb2k.easy.lock.IDistributedLockService;
import io.github.harishb2k.easy.lock.interceptor.DistributedLockInterceptor;

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

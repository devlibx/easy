package io.github.harishb2k.easy.resilience.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.github.harishb2k.easy.resilience.IResilienceManager;
import io.github.harishb2k.easy.resilience.IResilienceProcessor;
import io.github.harishb2k.easy.resilience.ResilienceManager;
import io.github.harishb2k.easy.resilience.ResilienceProcessor;

public class ResilienceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IResilienceManager.class).to(ResilienceManager.class).in(Scopes.SINGLETON);
        bind(IResilienceProcessor.class).to(ResilienceProcessor.class);
    }
}

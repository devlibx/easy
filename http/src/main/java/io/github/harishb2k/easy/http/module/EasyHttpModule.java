package io.github.harishb2k.easy.http.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.async.AsyncRequestProcessor;
import io.github.harishb2k.easy.http.helper.AsyncHttpClientBuilder;
import io.github.harishb2k.easy.http.helper.HttpClientBuilder;
import io.github.harishb2k.easy.http.helper.IClientBuilder;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.http.sync.DefaultHttpResponseProcessor;
import io.github.harishb2k.easy.http.sync.IHttpResponseProcessor;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;

public class EasyHttpModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ApiRegistry.class).in(Scopes.SINGLETON);
        bind(ServerRegistry.class).in(Scopes.SINGLETON);
        bind(IHttpResponseProcessor.class).to(DefaultHttpResponseProcessor.class);
        bind(IRequestProcessor.class).annotatedWith(Sync.class).to(SyncRequestProcessor.class).in(Scopes.SINGLETON);
        bind(IRequestProcessor.class).annotatedWith(Async.class).to(AsyncRequestProcessor.class).in(Scopes.SINGLETON);
        bind(IClientBuilder.class).annotatedWith(Sync.class).to(HttpClientBuilder.class).in(Scopes.SINGLETON);
        bind(IClientBuilder.class).annotatedWith(Async.class).to(AsyncHttpClientBuilder.class).in(Scopes.SINGLETON);
    }
}

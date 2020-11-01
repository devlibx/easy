package io.github.harishb2k.easy.database.mysql.module;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import io.gitbub.harishb2k.easy.helper.healthcheck.IHealthCheckProvider;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.DataSourceFactory;
import io.github.harishb2k.easy.database.mysql.DataSourceProxy;
import io.github.harishb2k.easy.database.mysql.DatabaseService;
import io.github.harishb2k.easy.database.mysql.IMysqlHelper;
import io.github.harishb2k.easy.database.mysql.MySqlHelper;
import io.github.harishb2k.easy.database.mysql.healthcheck.MySqlHealthCheckProvider;
import io.github.harishb2k.easy.database.mysql.transaction.ITransactionManagerResolver;
import io.github.harishb2k.easy.database.mysql.transaction.ITransactionManagerResolver.DefaultTransactionManagerResolver;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionInterceptor;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * This module provides all the dependencies to support MySQL related functions.
 *
 * <pre>
 * Yoy can customize the following:
 *
 * {@link TransactionInterceptor} - this class handles how methods annotated with @{@link Transactional}
 * handle transactions
 *
 * {@link ITransactionManagerResolver} - this class resolves the transaction manager to be used by methods annotated by
 * {@link Transactional}
 *
 *  NOTE - by default transaction aware data source is enabled. You must mark it false if you don't want to use it:
 *
 *  If you want to disable it then you must set "enable-transaction-aware-datasource=false" using following code
 *  <code>
 *  OptionalBinder.newOptionalBinder(binder(), Key.get(Boolean.class, Names.named("enable-transaction-aware-datasource")))
 *      .setBinding()
 *      .toInstance(Boolean.FALSE);
 * </code>
 *
 * Also use DatabaseMySQLModule(false) to disable transaction manager.
 */
public class DatabaseMySQLModule extends AbstractModule {
    private final boolean enableTransactionInterceptor;
    private final int defaultTimeout;

    public DatabaseMySQLModule() {
        enableTransactionInterceptor = true;
        defaultTimeout = 10;
    }

    public DatabaseMySQLModule(boolean enableTransactionInterceptor, int defaultTimeout) {
        this.enableTransactionInterceptor = enableTransactionInterceptor;
        this.defaultTimeout = defaultTimeout;
    }

    @Override
    protected void configure() {

        bind(DataSource.class).to(DataSourceProxy.class).in(Scopes.SINGLETON);
        bind(DataSourceFactory.class).in(Scopes.SINGLETON);
        bind(IMysqlHelper.class).to(MySqlHelper.class).in(Scopes.SINGLETON);
        bind(IDatabaseService.class).to(DatabaseService.class).in(Scopes.SINGLETON);

        // Set transaction aware data source as true by default
        OptionalBinder.newOptionalBinder(binder(), Key.get(Boolean.class, Names.named("enable-transaction-aware-datasource")))
                .setDefault()
                .toInstance(Boolean.TRUE);

        if (enableTransactionInterceptor) {
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class), transactionInterceptor());
        }

        // Provide health check implementation
        MapBinder<String, IHealthCheckProvider> healthCheckProviderMultiBinder = MapBinder.newMapBinder(binder(), String.class, IHealthCheckProvider.class);
        healthCheckProviderMultiBinder.permitDuplicates();
        healthCheckProviderMultiBinder.addBinding(healthCheckRegistrationName()).to(MySqlHealthCheckProvider.class);
    }

    protected String healthCheckRegistrationName() {
        return "MySQL";
    }

    @Provides
    @Singleton
    private ITransactionManagerResolver internalTransactionManagerResolver() {
        return transactionManagerResolver();
    }

    /**
     * @return DefaultTransactionManagerResolver - application can override this method to provide custom resolver.
     */
    protected ITransactionManagerResolver transactionManagerResolver() {
        return new DefaultTransactionManagerResolver();
    }

    /***
     * @return TransactionInterceptor which will handle method annotated with @{@link Transactional}
     */
    protected TransactionInterceptor transactionInterceptor() {
        return new TransactionInterceptor(defaultTimeout, getProvider(ITransactionManagerResolver.class));
    }
}

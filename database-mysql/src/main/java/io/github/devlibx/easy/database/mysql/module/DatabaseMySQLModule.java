package io.github.devlibx.easy.database.mysql.module;

import javax.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import io.gitbub.devlibx.easy.helper.healthcheck.IHealthCheckProvider;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.database.IDatabaseService;
import io.github.devlibx.easy.database.mysql.DataSourceFactory;
import io.github.devlibx.easy.database.mysql.DataSourceProxy;
import io.github.devlibx.easy.database.mysql.DatabaseService;
import io.github.devlibx.easy.database.mysql.IMysqlHelper;
import io.github.devlibx.easy.database.mysql.MySqlHelper;
import io.github.devlibx.easy.database.mysql.healthcheck.MySqlHealthCheckProvider;
import io.github.devlibx.easy.database.mysql.lock.MySqlLockBuilder;
import io.github.devlibx.easy.database.mysql.transaction.ITransactionManagerResolver;
import io.github.devlibx.easy.database.mysql.transaction.ITransactionManagerResolver.DefaultTransactionManagerResolver;
import io.github.devlibx.easy.database.mysql.transaction.TransactionInterceptor;
import io.github.devlibx.easy.lock.ILockBuilder;
import io.github.devlibx.easy.lock.module.LockModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.jodatime2.JodaTimePlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Types;

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
public class DatabaseMySQLModule extends LockModule {
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
        super.configure();

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

        // Provide mysql lock builder check implementation
        MapBinder<String, ILockBuilder> lockBuilderMapBinder = MapBinder.newMapBinder(binder(), String.class, ILockBuilder.class);
        lockBuilderMapBinder.permitDuplicates();
        lockBuilderMapBinder.addBinding("MYSQL").to(MySqlLockBuilder.class);

        // Set default lock table name
        OptionalBinder.newOptionalBinder(binder(), Key.get(String.class, Names.named("lock_table_name"))).setDefault().toInstance("locks");

        // Set default datasource table name
        OptionalBinder.newOptionalBinder(binder(), Key.get(DataSource.class, Names.named("lock_table_data_source"))).setDefault().to(DataSourceProxy.class);
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

    @Provides
    @Inject
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new JodaTimePlugin());
        jdbi.installPlugin(new GuavaPlugin());
        jdbi.registerArgument(new StringObjectMapArgumentFactory());
        return jdbi;
    }

    public static class StringObjectMapArgumentFactory extends AbstractArgumentFactory<StringObjectMap> {
        public StringObjectMapArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(StringObjectMap value, ConfigRegistry config) {
            return (position, statement, ctx) -> statement.setString(position, JsonUtils.asJson(value));
        }
    }
}

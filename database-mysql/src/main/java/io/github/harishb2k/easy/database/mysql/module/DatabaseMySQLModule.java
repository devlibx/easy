package io.github.harishb2k.easy.database.mysql.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.DataSourceFactory;
import io.github.harishb2k.easy.database.mysql.DataSourceProxy;
import io.github.harishb2k.easy.database.mysql.DatabaseService;
import io.github.harishb2k.easy.database.mysql.IMysqlHelper;
import io.github.harishb2k.easy.database.mysql.MySqlHelper;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionInterceptor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

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

        if (enableTransactionInterceptor) {
            bindConstant().annotatedWith(Names.named("transaction-aware-datasource")).to(true);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class), new TransactionInterceptor(defaultTimeout));
        } else {
            bindConstant().annotatedWith(Names.named("transaction-aware-datasource")).to(false);
        }
    }
}

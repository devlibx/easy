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
import io.github.harishb2k.easy.database.mysql.TransactionInterceptor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

public class DatabaseMySQLModule extends AbstractModule {
    private final boolean enableTransactionInterceptor;

    public DatabaseMySQLModule() {
        enableTransactionInterceptor = true;
    }

    public DatabaseMySQLModule(boolean enableTransactionInterceptor) {
        this.enableTransactionInterceptor = enableTransactionInterceptor;
    }

    @Override
    protected void configure() {
        bind(DataSource.class).to(DataSourceProxy.class).in(Scopes.SINGLETON);
        bind(DataSourceFactory.class).in(Scopes.SINGLETON);
        bind(IMysqlHelper.class).to(MySqlHelper.class).in(Scopes.SINGLETON);
        bind(IDatabaseService.class).to(DatabaseService.class).in(Scopes.SINGLETON);

        if (enableTransactionInterceptor) {
            bind(Boolean.class).annotatedWith(Names.named("transaction-aware-datasource")).toInstance(Boolean.TRUE);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class), new TransactionInterceptor());
        } else {
            bind(Boolean.class).annotatedWith(Names.named("transaction-aware-datasource")).toInstance(Boolean.FALSE);
        }
    }
}

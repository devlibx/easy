package io.github.harishb2k.easy.database.mysql.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.DataSourceFactory;
import io.github.harishb2k.easy.database.mysql.DataSourceProxy;
import io.github.harishb2k.easy.database.mysql.DatabaseService;
import io.github.harishb2k.easy.database.mysql.IMysqlHelper;
import io.github.harishb2k.easy.database.mysql.MySqlHelper;

import javax.sql.DataSource;

public class DatabaseMySQLModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DataSource.class).to(DataSourceProxy.class).in(Scopes.SINGLETON);
        bind(DataSourceFactory.class).in(Scopes.SINGLETON);
        bind(IMysqlHelper.class).to(MySqlHelper.class).in(Scopes.SINGLETON);
        bind(IDatabaseService.class).to(DatabaseService.class).in(Scopes.SINGLETON);
    }
}

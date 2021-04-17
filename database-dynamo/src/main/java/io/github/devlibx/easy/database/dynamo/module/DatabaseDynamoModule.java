package io.github.devlibx.easy.database.dynamo.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.github.devlibx.easy.database.IDatabaseService;
import io.github.devlibx.easy.database.dynamo.DataSourceFactory;
import io.github.devlibx.easy.database.dynamo.DatabaseService;
import io.github.devlibx.easy.database.dynamo.DynamoHelper;
import io.github.devlibx.easy.database.dynamo.IDynamoHelper;

public class DatabaseDynamoModule extends AbstractModule {
    @Override
    protected void configure() {
        super.configure();
        bind(IDynamoHelper.class).to(DynamoHelper.class).in(Scopes.SINGLETON);
        bind(IDatabaseService.class).to(DatabaseService.class).in(Scopes.SINGLETON);
        bind(DataSourceFactory.class).in(Scopes.SINGLETON);
    }
}

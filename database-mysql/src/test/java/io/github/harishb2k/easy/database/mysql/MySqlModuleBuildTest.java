package io.github.harishb2k.easy.database.mysql;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.github.harishb2k.easy.database.mysql.module.DatabaseMySQLModule;
import io.github.harishb2k.easy.database.mysql.transaction.ITransactionManagerResolver;
import junit.framework.TestCase;


public class MySqlModuleBuildTest extends TestCase {

    public void testMySqlModuleBuildSuccess() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
            }
        }, new DatabaseMySQLModule());

        ITransactionManagerResolver transactionManagerResolver = injector.getInstance(ITransactionManagerResolver.class);
        assertNotNull(transactionManagerResolver);
    }
}

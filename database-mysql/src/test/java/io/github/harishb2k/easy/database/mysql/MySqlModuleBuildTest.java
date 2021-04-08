package io.github.devlibx.easy.database.mysql;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.CommonBaseTestCase;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.database.mysql.module.DatabaseMySQLModule;
import io.github.devlibx.easy.database.mysql.transaction.ITransactionManagerResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * This test is to make sure we do not have issue in our module
 */
public class MySqlModuleBuildTest extends CommonBaseTestCase {

    @Test
    @DisplayName("Make sure that DatabaseMySQLModule can be added to guice")
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

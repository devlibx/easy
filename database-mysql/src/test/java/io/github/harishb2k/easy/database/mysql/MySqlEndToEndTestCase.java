package io.github.harishb2k.easy.database.mysql;

import ch.qos.logback.classic.Level;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.CommonBaseTestCase;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.TransactionSupportTestWithTwoDataSource.HelperToTestTransactionWithTwoDatasource;
import io.github.harishb2k.easy.database.mysql.TransactionSupportTestWithTwoDataSource.IHelperToTestTransactionWithTwoDatasource;
import io.github.harishb2k.easy.database.mysql.TransactionTest.ITransactionTestClass;
import io.github.harishb2k.easy.database.mysql.TransactionTest.TransactionTestClass;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfig;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfigs;
import io.github.harishb2k.easy.database.mysql.module.DatabaseMySQLModule;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionContext;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionInterceptor;
import io.github.harishb2k.easy.lock.interceptor.DistributedLockInterceptor;
import io.github.harishb2k.easy.testing.mysql.TestingMySqlDataSource;
import io.github.harishb2k.easy.testing.mysql.MySqlExtension;
import io.github.harishb2k.easy.testing.mysql.TestingMySqlConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.com.github.dockerjava.core.command.AbstrDockerCmd;

import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;

@SuppressWarnings({"all"})
@Slf4j
public class MySqlEndToEndTestCase extends CommonBaseTestCase {
    private static Injector injector;

    @RegisterExtension
    public static MySqlExtension defaultMysql = MySqlExtension.builder()
            .withDatabase("users")
            .withHost("localhost")
            .withUsernamePassword("test", "test")
            .build();

    @RegisterExtension
    public static MySqlExtension otherMySQL = MySqlExtension.builder("other")
            .withDatabase("test_me")
            .withHost("localhost")
            .withUsernamePassword("test", "test")
            .build();

    public static void startMySQL() throws RuntimeException {
    }

    @AfterAll
    public static void stopMySQL() {
    }

    @AfterEach
    public void tearDown() throws Exception {
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.stopDatabase();
    }

    public void setupGuice(TestingMySqlConfig primaryMySqlConfig,
                           TestingMySqlConfig secondaryMySqlConfig) {

        // Setup DB - datasource
        MySqlConfig dbConfig = new MySqlConfig();
        dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfig.setJdbcUrl(primaryMySqlConfig.getJdbcUrl());
        dbConfig.setUsername(primaryMySqlConfig.getUsername());
        dbConfig.setPassword(primaryMySqlConfig.getPassword());
        dbConfig.setShowSql(false);
        MySqlConfigs mySqlConfigs = new MySqlConfigs();
        mySqlConfigs.addConfig(dbConfig);

        MySqlConfig dbConfigSecondary = new MySqlConfig();
        dbConfigSecondary.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfigSecondary.setJdbcUrl(secondaryMySqlConfig.getJdbcUrl());
        dbConfigSecondary.setUsername(secondaryMySqlConfig.getUsername());
        dbConfigSecondary.setPassword(secondaryMySqlConfig.getPassword());
        dbConfigSecondary.setShowSql(false);
        mySqlConfigs.addConfig("secondary", dbConfigSecondary);

        // Setup module
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                bind(MySqlConfigs.class).toInstance(mySqlConfigs);
                bind(ITransactionTestClass.class).to(TransactionTestClass.class).in(Scopes.SINGLETON);
                bind(IHelperToTestTransactionWithTwoDatasource.class).to(HelperToTestTransactionWithTwoDatasource.class).in(Scopes.SINGLETON);
                bind(SimpleMysqlHelperTest.class).toInstance(new SimpleMysqlHelperTest() {
                });
            }
        }, new DatabaseMySQLModule());
        ApplicationContext.setInjector(injector);

        // Start DB
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();
    }

    @Test
    @DisplayName("This will run all MySQL tests")
    public void completeMySQLTest(
            TestingMySqlConfig primaryMySqlConfig,
            @TestingMySqlDataSource("other") TestingMySqlConfig secondaryMySqlConfig
    ) throws Exception {
        Assumptions.assumeTrue(primaryMySqlConfig.isRunning());

        // Setup logging
        try {
            LoggingHelper.setupLogging();
            LoggingHelper.getLogger(TransactionInterceptor.class).setLevel(INFO);
            LoggingHelper.getLogger(DistributedLockInterceptor.class).setLevel(TRACE);
            LoggingHelper.getLogger(HikariPool.class).setLevel(Level.OFF);
            LoggingHelper.getLogger(HikariConfig.class).setLevel(Level.OFF);
            LoggingHelper.getLogger(AbstrDockerCmd.class).setLevel(INFO);
            LoggingHelper.getLogger("org.testcontainser").setLevel(INFO);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        // Start MySQL
        startMySQL();

        // Setup dependencies
        setupGuice(primaryMySqlConfig, secondaryMySqlConfig);

        // Test 1 - Test default DB
        SimpleMysqlHelperTest simpleMysqlHelperTest = injector.getInstance(SimpleMysqlHelperTest.class);
        simpleMysqlHelperTest.runTest();

        // Test 2 - Test secondary DB if enabled
        if (secondaryMySqlConfig.isRunning()) {
            try {
                TransactionContext.getInstance().getContext().setDatasourceName("secondary");
                simpleMysqlHelperTest.runTest();
            } finally {
                TransactionContext.getInstance().clear();
            }
        }

        // Test 3 - Test transaction code
        TransactionTest transactionTest = new TransactionTest() {
        };
        transactionTest.testTransaction();

        // Test 4 - Test Multi Db
        if (secondaryMySqlConfig.isRunning()) {
            validateMultiDb();
        }

        // Test 5 - Test transaction code
        transactionTest.testTransactionBulk();

        // Close MySQL
        stopMySQL();
    }

    private static void validateMultiDb() {
        TransactionSupportTestWithTwoDataSource transactionSupportTestWithTwoDataSource = new TransactionSupportTestWithTwoDataSource(injector) {
        };
        transactionSupportTestWithTwoDataSource.runTest();
    }
}

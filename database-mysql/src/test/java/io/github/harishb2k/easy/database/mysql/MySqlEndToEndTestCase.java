package io.github.harishb2k.easy.database.mysql;

import ch.qos.logback.classic.Level;
import com.github.dockerjava.core.command.AbstrDockerCmd;
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
import io.gitbub.harishb2k.easy.helper.mysql.IMySqlTestHelper;
import io.gitbub.harishb2k.easy.helper.mysql.IMySqlTestHelper.TestMySqlConfig;
import io.gitbub.harishb2k.easy.helper.mysql.MySqlTestHelper;
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
import lombok.extern.slf4j.Slf4j;

import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;

@SuppressWarnings({"all"})
@Slf4j
public class MySqlEndToEndTestCase extends CommonBaseTestCase {
    private static String jdbcUrl = "jdbc:mysql://localhost:3306/users?useSSL=false";
    private static String secondaryJdbcUrl = "jdbc:mysql://localhost:3306/test_me?useSSL=false";
    private static Injector injector;
    private static IMySqlTestHelper primaryMySqlTestHelper;
    private static IMySqlTestHelper secondaryMySqlTestHelper;


    public static void startMySQL() throws RuntimeException {
        // Primary setup
        primaryMySqlTestHelper = new MySqlTestHelper();
        primaryMySqlTestHelper.installCustomMySqlTestHelper(new MySQLHelperPlugin());
        TestMySqlConfig primaryMySqlConfig = TestMySqlConfig.withDefaults();
        primaryMySqlTestHelper.startMySql(primaryMySqlConfig);

        // Secondary setup
        secondaryMySqlTestHelper = new MySqlTestHelper();
        secondaryMySqlTestHelper.installCustomMySqlTestHelper(new MySQLHelperPlugin());
        TestMySqlConfig secondartMySqlConfig = TestMySqlConfig.withDefaults();
        secondaryMySqlTestHelper.startMySql(secondartMySqlConfig);
    }

    public static void stopMySQL() {
        primaryMySqlTestHelper.stopMySql();
        secondaryMySqlTestHelper.stopMySql();
    }

    public static void setupGuice() {

        // Setup DB - datasource
        MySqlConfig dbConfig = new MySqlConfig();
        dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfig.setJdbcUrl(jdbcUrl);
        dbConfig.setUsername("test");
        dbConfig.setPassword("test");
        dbConfig.setShowSql(false);
        MySqlConfigs mySqlConfigs = new MySqlConfigs();
        mySqlConfigs.addConfig(dbConfig);

        MySqlConfig dbConfigSecondary = new MySqlConfig();
        dbConfigSecondary.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfigSecondary.setJdbcUrl(secondaryJdbcUrl);
        dbConfigSecondary.setUsername("test");
        dbConfigSecondary.setPassword("test");
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

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.stopDatabase();
    }

    public void testMySQL() throws Exception {
        main(null);
    }

    public static void main(String[] args) throws Exception {
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
        setupGuice();

        // Test 1 - Test default DB
        SimpleMysqlHelperTest simpleMysqlHelperTest = injector.getInstance(SimpleMysqlHelperTest.class);
        simpleMysqlHelperTest.runTest();

        // Test 2 - Test secondary DB if enabled
        if (secondaryMySqlTestHelper.isMySqlRunning()) {
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
        if (secondaryMySqlTestHelper.isMySqlRunning()) {
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

package io.github.harishb2k.easy.database.mysql;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.Safe;
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
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

import java.util.UUID;

@SuppressWarnings({"all"})
@Slf4j
public abstract class ExampleApp extends TestCase {
    private static String jdbcUrl = "jdbc:mysql://localhost:3306/users?useSSL=false";
    private static String secondaryJdbcUrl = "jdbc:mysql://localhost:3306/test_me?useSSL=false";
    private static MySQLContainer container;
    private static MySQLContainer containerSecondary;
    private static Injector injector;
    private static String uniqueString = UUID.randomUUID().toString();

    private static boolean useDockerMySql = false;
    private static boolean testMultiDb = true;

    public static void startMySQL() throws RuntimeException {
        if (!useDockerMySql) return;

        container = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withEnv("MYSQL_ROOT_HOST", "%")
                .withExposedPorts(3306);
        try {
            container.start();
        } catch (ContainerLaunchException e) {
            throw new RuntimeException(e);
        }

        jdbcUrl = container.getJdbcUrl();

        containerSecondary = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withDatabaseName("test_me")
                .withUsername("test")
                .withPassword("test")
                .withEnv("MYSQL_ROOT_HOST", "%")
                .withExposedPorts(3306);
        try {
            containerSecondary.start();
        } catch (ContainerLaunchException e) {
            throw new RuntimeException(e);
        }
        secondaryJdbcUrl = containerSecondary.getJdbcUrl();
    }

    public static void stopMySQL() {
        if (!useDockerMySql) return;

        if (container != null) {
            try {
                injector.getInstance(IDatabaseService.class).stopDatabase();
                container.stop();
            } catch (Exception ignored) {
            }
        }

        if (containerSecondary != null) {
            Safe.safe(() -> containerSecondary.stop());
        }
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

        if (testMultiDb) {
            MySqlConfig dbConfigSecondary = new MySqlConfig();
            dbConfigSecondary.setDriverClassName("com.mysql.jdbc.Driver");
            dbConfigSecondary.setJdbcUrl(secondaryJdbcUrl);
            dbConfigSecondary.setUsername("test");
            dbConfigSecondary.setPassword("test");
            dbConfigSecondary.setShowSql(false);
            mySqlConfigs.addConfig("secondary", dbConfigSecondary);
        }

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

    public static void main(String[] args) {
        // Setup logging
        try {
            LoggingHelper.setupLogging();
            Logger.getLogger(TransactionInterceptor.class).setLevel(Level.TRACE);
            Logger.getLogger(HikariPool.class).setLevel(Level.OFF);
            Logger.getLogger(HikariConfig.class).setLevel(Level.OFF);
        } catch (Exception ignored) {
        }

        // Start MySQL
        startMySQL();

        // Setup dependencies
        setupGuice();

        // Test 1 - Test default DB
        SimpleMysqlHelperTest simpleMysqlHelperTest = injector.getInstance(SimpleMysqlHelperTest.class);
        simpleMysqlHelperTest.runTest();

        // Test 2 - Test secondary DB if enabled
        if (testMultiDb) {
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
        if (testMultiDb) {
            validateMultiDb();
        }

        // Close MySQL
        stopMySQL();
    }

    private static void validateMultiDb() {
        TransactionSupportTestWithTwoDataSource transactionSupportTestWithTwoDataSource = new TransactionSupportTestWithTwoDataSource(injector) {
        };
        transactionSupportTestWithTwoDataSource.runTest();
    }
}

package io.github.harishb2k.easy.database.mysql;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfig;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfigs;
import io.github.harishb2k.easy.database.mysql.module.DatabaseMySQLModule;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("rawtypes")
@Slf4j
public abstract class ExampleApp extends TestCase {
    private static String jdbcUrl = "jdbc:mysql://localhost:3306/users?useSSL=false";
    private static MySQLContainer container;
    private static Injector injector;
    private static boolean useDockerMySql = false;
    private static String uniqueString = UUID.randomUUID().toString();

    public static void startMySQL() throws RuntimeException {
        if (!useDockerMySql) return;

        container = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withDatabaseName("test")
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

        // Setup module
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                bind(MySqlConfigs.class).toInstance(mySqlConfigs);
                bind(ITransactionTestClass.class).to(TransactionTestClass.class).in(Scopes.SINGLETON);
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

        // Step 1 - Create a DB
        IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
        boolean executeResult = mysqlHelper.execute(
                "",
                "CREATE TABLE IF NOT EXISTS users (ID int NOT NULL PRIMARY KEY AUTO_INCREMENT, name varchar(255)); ",
                preparedStatement -> {
                }
        );
        Assert.assertFalse(executeResult);


        // Step 2 - Insert to DB
        Long id = mysqlHelper.persist(
                "",
                "INSERT INTO users(name) VALUES(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, "HI");
                }
        );
        Assert.assertNotNull(id);
        Assert.assertTrue(id > 0);

        // Try insert in threads
        int count = 0;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                Long _id = mysqlHelper.persist(
                        "",
                        "INSERT INTO users(name) VALUES(?)",
                        preparedStatement -> {
                            preparedStatement.setString(1, "HI");
                        }
                );
                countDownLatch.countDown();
                Assert.assertNotNull(_id);
                Assert.assertTrue(_id > 0);
            }).start();
        }
        try {
            if (count > 0) {
                countDownLatch.await();
            }
        } catch (InterruptedException ignored) {
        }

        // Step 2 - Read from DB
        String result = mysqlHelper.findOne(
                "",
                "SELECT name from users",
                statement -> {
                },
                rs -> rs.getString(1),
                String.class
        ).orElse("");
        Assert.assertEquals("HI", result);
        log.debug("Result from MySQL Select: {}", result);


        // Test transaction code
        testTransaction();

        // Close MySQL
        stopMySQL();
    }

    private static void testTransaction() {
        ITransactionTestClass transactionTestClass = injector.getInstance(ITransactionTestClass.class);
        try {
            transactionTestClass.persistRecordFirst();
        } catch (Exception e) {
            log.error("Expected error - " + e.getMessage());
        }

        IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
        List<String> results = mysqlHelper.findAll(
                "na",
                "SELECT name from users where name like ?",
                statement -> {
                    statement.setString(1, "%" + uniqueString);
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(new ArrayList<>());
        assertEquals(2, results.size());
        assertEquals("persistRecordSecond-" + uniqueString, results.get(0));
        assertEquals("persistRecordForth-" + uniqueString, results.get(1));

        results.forEach(s -> {
            log.info("Result after transaction = " + s);
        });

        transactionTestClass.persistWithoutTransaction();
        results = mysqlHelper.findAll(
                "na",
                "SELECT name from users where name like ?",
                statement -> {
                    statement.setString(1, "persistWithoutTransaction-" + uniqueString);
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(new ArrayList<>());
        assertEquals(1, results.size());
        assertEquals("persistWithoutTransaction-" + uniqueString, results.get(0));
    }

    interface ITransactionTestClass {
        Long persistRecordFirst();

        Long persistRecordSecond();

        Long persistRecordThird();

        Long persistRecordForth();

        Long persistWithoutTransaction();
    }


    public static class TransactionTestClass implements ITransactionTestClass {
        private final IMysqlHelper mysqlHelper;

        @Inject
        public TransactionTestClass(IMysqlHelper mysqlHelper) {
            this.mysqlHelper = mysqlHelper;
        }

        @Override
        @Transactional(value = "persistRecordFirst", propagation = Propagation.REQUIRED)
        public Long persistRecordFirst() {
            mysqlHelper.persist(
                    "none",
                    "INSERT INTO USERS(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordFirst-" + uniqueString);
                    }
            );

            ITransactionTestClass transactionTestClass = ApplicationContext.getInstance(ITransactionTestClass.class);
            transactionTestClass.persistRecordSecond();
            transactionTestClass.persistRecordThird();
            transactionTestClass.persistRecordForth();

            throw new RuntimeException("Expected error - Generate exception to fail this transaction");
        }

        @Override
        @Transactional(value = "persistRecordSecond", propagation = Propagation.REQUIRES_NEW)
        public Long persistRecordSecond() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO USERS(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordSecond-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(value = "persistRecordThird", propagation = Propagation.REQUIRED)
        public Long persistRecordThird() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO USERS(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordThird-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(value = "persistRecordForth", propagation = Propagation.REQUIRES_NEW)
        public Long persistRecordForth() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO USERS(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordForth-" + uniqueString);
                    }
            );
        }

        @Override
        public Long persistWithoutTransaction() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO USERS(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistWithoutTransaction-" + uniqueString);
                    }
            );
        }
    }
}

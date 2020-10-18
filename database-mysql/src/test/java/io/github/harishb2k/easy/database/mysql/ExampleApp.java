package io.github.harishb2k.easy.database.mysql;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.config.DbConfig;
import io.github.harishb2k.easy.database.mysql.module.DatabaseMySQLModule;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

@SuppressWarnings("rawtypes")
@Slf4j
public abstract class ExampleApp extends TestCase {
    private static String jdbcUrl;
    private static MySQLContainer container;
    private static Injector injector;

    public static void startMySQL() throws RuntimeException {
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
    }

    public static void stopMySQL() {
        if (container != null) {
            try {
                container.stop();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void setupGuice() {

        // Setup module
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
            }
        }, new DatabaseMySQLModule());
        ApplicationContext.setInjector(injector);

        // Setup DB - datasource
        DbConfig dbConfig = new DbConfig();
        dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfig.setJdbcUrl(container.getJdbcUrl() + "?logger=Slf4JLogger&profileSQL=true&profileSQL=true");
        dbConfig.setUsername("test");
        dbConfig.setPassword("test");
        injector.getInstance(DataSourceFactory.class).register(dbConfig.buildHikariDataSource());

        // Start DB
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();

    }

    public static void main(String[] args) {
        // Start MySQL
        startMySQL();

        // Setup dependencies
        setupGuice();

        // Step 1 - Create a DB
        IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
        boolean executeResult = mysqlHelper.execute(
                "",
                "CREATE TABLE ta (ID int NOT NULL PRIMARY KEY AUTO_INCREMENT, a varchar(255)); ",
                preparedStatement -> {
                }
        );
        Assert.assertFalse(executeResult);

        // Step 2 - Insert to DB
        Long id = mysqlHelper.persist(
                "",
                "INSERT INTO ta(a) VALUES(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, "HI");
                }
        );
        Assert.assertNotNull(id);
        Assert.assertTrue(id > 0);

        // Step 2 - Read from DB
        String result = mysqlHelper.findOne(
                "",
                "SELECT a from ta",
                statement -> {
                },
                rs -> rs.getString(1),
                String.class
        ).orElse("");
        Assert.assertEquals("HI", result);
        log.debug("Result from MySQL Select: {}", result);

        // Close MySQL
        stopMySQL();
    }
}

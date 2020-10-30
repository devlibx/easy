package io.github.harishb2k.easy.database.mysql;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Slf4j
public abstract class TransactionSupportTestWithTwoDataSource {
    private final Injector injector;
    private static String uniqueString;

    public TransactionSupportTestWithTwoDataSource(Injector injector) {
        this.injector = injector;
        uniqueString = UUID.randomUUID().toString();
    }

    public void runTest() {
        log.debug("\n\nStart:TransactionSupportTestWithTwoDataSource:\n");
        IHelperToTestTransactionWithTwoDatasource helper = injector.getInstance(IHelperToTestTransactionWithTwoDatasource.class);
        helper.testPersistToFirstDb();
        helper.testPersistToSecondDb();

        DataSourceFactory dataSourceFactory = injector.getInstance(DataSourceFactory.class);
        DataSource defaultDataSource = dataSourceFactory.getDataSource("default");
        DataSource secondaryDataSource = dataSourceFactory.getDataSource("secondary");

        String dataFromDefaultDataSource = getResult("testPersistToFirstDb", defaultDataSource);
        log.debug("From default db {}", dataFromDefaultDataSource);
        Assert.assertEquals("testPersistToFirstDb-" + uniqueString, dataFromDefaultDataSource);

        String dataFromSecondaryDataSource = getResult("testPersistToSecondDb", secondaryDataSource);
        log.debug("From secondary db {}", dataFromSecondaryDataSource);
        Assert.assertEquals("testPersistToSecondDb-" + uniqueString, dataFromSecondaryDataSource);

        log.debug("\n\nEnd:TransactionSupportTestWithTwoDataSource:\n\n");
    }

    private String getResult(String prefix, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name from users where name like ?")) {
            statement.setString(1, prefix + "-" + uniqueString);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (Exception e) {
            log.error("Unexpected error ", e);
        }
        return null;
    }

    public interface IHelperToTestTransactionWithTwoDatasource {
        Long testPersistToFirstDb();

        Long testPersistToSecondDb();
    }

    public static class HelperToTestTransactionWithTwoDatasource implements IHelperToTestTransactionWithTwoDatasource {
        private final IMysqlHelper mysqlHelper;

        @Inject
        public HelperToTestTransactionWithTwoDatasource(IMysqlHelper mysqlHelper) {
            this.mysqlHelper = mysqlHelper;
        }

        @Override
        @Transactional(label = {"dataSourceName=default"}, propagation = Propagation.REQUIRES_NEW)
        public Long testPersistToFirstDb() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "testPersistToFirstDb-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(label = {"dataSourceName=secondary"}, propagation = Propagation.REQUIRES_NEW)
        public Long testPersistToSecondDb() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "testPersistToSecondDb-" + uniqueString);
                    }
            );
        }
    }
}

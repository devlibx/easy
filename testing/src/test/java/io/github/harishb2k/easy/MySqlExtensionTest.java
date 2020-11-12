package io.github.harishb2k.easy;

import io.github.harishb2k.easy.testing.mysql.TestingMySqlConfig;
import io.github.harishb2k.easy.testing.mysql.TestingMySqlDataSource;
import io.github.harishb2k.easy.testing.mysql.MySqlExtension;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MySqlExtensionTest {

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

    @Test
    @DisplayName("Default mysql extension with default settings")
    public void testDefaultMySQL(TestingMySqlConfig mySqlConfig, DataSource dataSource) {
        // Make sure mysql is running
        Assumptions.assumeTrue(mySqlConfig.isRunning());

        internalTestMySQL(mySqlConfig, dataSource);
    }

    @Test
    @DisplayName("Test a alternate datasource")
    public void testOtherMySQL(@TestingMySqlDataSource("other") TestingMySqlConfig mySqlConfig, @TestingMySqlDataSource("other") DataSource dataSource) {
        // Make sure mysql is running
        Assumptions.assumeTrue(mySqlConfig.isRunning());

        internalTestMySQL(mySqlConfig, dataSource);
    }

    @Test
    @DisplayName("Test both default and alternate datasource can work together")
    public void testDefaultAndOtherMySQL(TestingMySqlConfig mySqlConfig, DataSource dataSource, @TestingMySqlDataSource("other") DataSource dataSourceOther) {
        // Make sure mysql is running
        Assumptions.assumeTrue(mySqlConfig.isRunning());

        internalTestMySQL(mySqlConfig, dataSource);
        internalTestMySQL(mySqlConfig, dataSourceOther);
    }

    public void internalTestMySQL(TestingMySqlConfig mySqlConfig, DataSource dataSource) {
        boolean ranAll = false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement create = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users (ID int NOT NULL PRIMARY KEY AUTO_INCREMENT, name varchar(255));");
             PreparedStatement insert = connection.prepareStatement("INSERT INTO users(name) VALUES(?);");
             PreparedStatement select = connection.prepareStatement("SELECT id, name from users WHERE name=?;");
        ) {

            // Create table
            create.execute();

            // Insert row
            String name = UUID.randomUUID().toString();
            insert.setString(1, name);
            insert.execute();

            // Select
            select.setString(1, name);
            ResultSet resultSet = select.executeQuery();
            assertTrue(resultSet.next());
            assertTrue(resultSet.getLong(1) > 0);
            assertEquals(name, resultSet.getString(2));

            ranAll = true;
        } catch (Exception ignored) {
        }
        assertTrue(ranAll);
    }
}

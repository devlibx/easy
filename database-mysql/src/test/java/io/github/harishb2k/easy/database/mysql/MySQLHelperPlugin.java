package io.github.harishb2k.easy.database.mysql;

import com.zaxxer.hikari.HikariDataSource;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.gitbub.harishb2k.easy.helper.mysql.IMySqlTestHelper;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfig;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Slf4j
public class MySQLHelperPlugin implements IMySqlTestHelper {
    private TestMySqlConfig config;
    private DataSource dataSource;

    @Override
    public void startMySql(TestMySqlConfig config) {
        this.config = config;
        MySqlConfig dbConfig = new MySqlConfig();
        dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfig.setJdbcUrl(config.getJdbcUrl());
        dbConfig.setUsername(config.getUser());
        dbConfig.setPassword(config.getPassword());
        dbConfig.setShowSql(false);
        dataSource = dbConfig.buildHikariDataSource();
    }

    @Override
    public void stopMySql() {
        Safe.safe(() -> ((HikariDataSource) dataSource).close());
    }

    @Override
    public TestMySqlConfig getMySqlConfig() {
        return config;
    }

    @Override
    public boolean isMySqlRunning() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1;")) {
            preparedStatement.executeQuery();
        } catch (Exception e) {
            log.error("(Ignore this error ) MySQLHelperPlugin - (checking MySql is running in your system - to run test cases) - {}", e.getMessage());
            return false;
        }
        return true;
    }
}

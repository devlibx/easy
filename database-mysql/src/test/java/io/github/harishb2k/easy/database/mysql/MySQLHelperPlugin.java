package io.github.harishb2k.easy.database.mysql;

import io.gitbub.harishb2k.easy.helper.MySQLHelper.IMySQLHelperPlugin;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfig;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Slf4j
public class MySQLHelperPlugin implements IMySQLHelperPlugin {
    private DataSource dataSource;
    private String jdbcUrl;
    private String userName;
    private String password;

    @Override
    public boolean startMySQL(String defaultJdbcUrl, String userName, String password) {
        jdbcUrl = defaultJdbcUrl;
        this.userName = userName;
        this.password = password;
        MySqlConfig dbConfig = new MySqlConfig();
        dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfig.setJdbcUrl(defaultJdbcUrl);
        dbConfig.setUsername(getUserName());
        dbConfig.setPassword(getPassword());
        dbConfig.setShowSql(false);
        dataSource = dbConfig.buildHikariDataSource();
        return canRunMySQL();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public boolean canRunMySQL() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1;")) {
            preparedStatement.executeQuery();
        } catch (Exception e) {
            log.error("Failed ", e);
            return false;
        }
        return true;
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }
}

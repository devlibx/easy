package io.github.harishb2k.easy.database.mysql.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import lombok.Data;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

@Data
public class DbConfig {
    private String catalog;
    private String connectionInitSql;
    private String connectionTestQuery;
    private String dataSourceClassName;
    private String dataSourceJndiName;
    private String driverClassName;
    private String jdbcUrl;
    private String password;
    private String poolName;
    private String transactionIsolationName;
    private String username;
    private boolean isAutoCommit;
    private boolean isReadOnly;
    private boolean isInitializationFailFast;
    private boolean isIsolateInternalQueries;
    private boolean isRegisterMbeans;
    private boolean isAllowPoolSuspension;
    private DataSource dataSource;
    private Properties dataSourceProperties;
    private ThreadFactory threadFactory;
    private ScheduledThreadPoolExecutor scheduledExecutor;
    private MetricsTrackerFactory metricsTrackerFactory;
    private Object metricRegistry;
    private Object healthCheckRegistry;
    private Properties healthCheckProperties;

    public HikariDataSource buildHikariDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDataSourceClassName(dataSourceClassName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}

package io.github.harishb2k.easy.database.mysql.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;

@Data
public class DbConfig {
    private boolean isAutoCommit;
    private String driverClassName;
    private String jdbcUrl;
    private String username;
    private String password;
    private long idleTimeout;
    private int maxPoolSize = 10;
    private long leakDetectionThreshold;
    private boolean useLocalSessionState;
    private boolean useUsageAdvisor;
    private boolean showSql;

    public HikariDataSource buildHikariDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setAutoCommit(true);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setMaximumPoolSize(maxPoolSize);
        dataSource.setLeakDetectionThreshold(leakDetectionThreshold);
        dataSource.addDataSourceProperty("useLocalSessionState", useLocalSessionState);
        dataSource.addDataSourceProperty("useUsageAdvisor", useUsageAdvisor);

        // Used for logging/debugging
        if (showSql) {
            dataSource.addDataSourceProperty("logger", "Slf4JLogger");
            dataSource.addDataSourceProperty("profilerEventHandler", "io.github.harishb2k.easy.database.mysql.debug.DoNotUseProfilerEventHandler");
            dataSource.addDataSourceProperty("profileSQL", true);
        }

        return dataSource;
    }
}

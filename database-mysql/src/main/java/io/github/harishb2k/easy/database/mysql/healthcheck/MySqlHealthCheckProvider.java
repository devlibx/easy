package io.github.devlibx.easy.database.mysql.healthcheck;

import io.gitbub.devlibx.easy.helper.healthcheck.IHealthCheckProvider;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.database.mysql.DataSourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MySqlHealthCheckProvider implements IHealthCheckProvider {
    private final DataSourceFactory dataSourceFactory;

    @Inject
    public MySqlHealthCheckProvider(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public Result check() {
        StringBuilder sb = new StringBuilder();
        StringObjectMap message = new StringObjectMap();
        AtomicBoolean errorFound = new AtomicBoolean(false);

        try {

            dataSourceFactory.getDataSourceMap().forEach((name, _dataSource) -> {

                // Get the datasource
                DataSource dataSource = _dataSource;
                if (_dataSource instanceof TransactionAwareDataSourceProxy) {
                    dataSource = ((TransactionAwareDataSourceProxy) _dataSource).getTargetDataSource();
                }

                // If datasource is null then it is a error
                if (dataSource == null) {
                    message.put(name, "Failed to get datasource for " + name);
                    errorFound.set(true);
                    return;
                }

                // Run SQL to check database connection
                try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1");) {
                    statement.execute();
                    message.put(name, "OK");
                    sb.append(name).append("[").append("OK").append("], ");
                } catch (Exception e) {
                    log.error("Failed to connect to {} DB", name, e);
                    message.put(name, "Failed to connect to " + name + " DB: " + e.getMessage());
                    errorFound.set(true);
                    sb.append(name).append("[").append(e.getMessage()).append("], ");
                }

            });
        } catch (Exception e) {
            return Result.builder()
                    .healthy(false)
                    .details(message)
                    .error(e)
                    .message("Failed to check health of DB - " + e.getMessage())
                    .build();
        }

        if (errorFound.get()) {
            return Result.builder()
                    .healthy(false)
                    .details(message)
                    .message(sb.toString() + " DB(s) failed")
                    .build();
        } else {
            return Result.builder()
                    .healthy(true)
                    .details(message)
                    .message(sb.toString() + " DB(s) Ok")
                    .build();
        }
    }
}

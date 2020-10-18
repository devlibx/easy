package io.github.harishb2k.easy.database.mysql;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DataSourceFactory {
    private final Map<String, DataSource> dataSourceMap;

    public DataSourceFactory() {
        this.dataSourceMap = new HashMap<>();
    }

    public void register(DataSource dataSource) {
        dataSourceMap.put("default", dataSource);
    }

    public void register(String dataSourceName, DataSource dataSource) {
        dataSourceMap.put(dataSourceName, dataSource);
    }

    public DataSource getDataSource(String dataSourceName) {
        return dataSourceMap.get(dataSourceName);
    }

    public DataSource getDataSource() {
        return dataSourceMap.get("default");
    }

    public void shutdown() {
        dataSourceMap.forEach((name, dataSource) -> {
            log.info("Close Datasource Begin: {}", name);
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        });
        dataSourceMap.clear();
    }
}

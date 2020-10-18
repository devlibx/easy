package io.github.harishb2k.easy.database.mysql;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

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
}

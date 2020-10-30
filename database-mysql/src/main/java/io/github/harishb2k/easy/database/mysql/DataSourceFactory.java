package io.github.harishb2k.easy.database.mysql;

import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionContext;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionContext.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.inject.Named;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class DataSourceFactory {
    private final Map<String, DataSource> dataSourceMap;

    @com.google.inject.Inject(optional = true)
    @Named("transaction-aware-datasource")
    private boolean transactionAwareDatasource = false;

    public DataSourceFactory() {
        this.dataSourceMap = new HashMap<>();
    }

    public void register(DataSource dataSource) {
        if (transactionAwareDatasource) {
            dataSourceMap.put("default", new TransactionAwareDataSourceProxy(dataSource));
        } else {
            dataSourceMap.put("default", dataSource);
        }
    }

    public void register(String dataSourceName, DataSource dataSource) {
        if (transactionAwareDatasource) {
            dataSourceMap.put(dataSourceName, new TransactionAwareDataSourceProxy(dataSource));
        } else {
            dataSourceMap.put(dataSourceName, dataSource);
        }
    }

    public DataSource getDataSource(String dataSourceName) {
        return dataSourceMap.get(dataSourceName);
    }

    public DataSource getDataSource() {
        Context context = TransactionContext.getInstance().getContext();
        if (context == null || Strings.isNullOrEmpty(context.getDatasourceName()) || Objects.equals("default", context.getDatasourceName())) {
            return dataSourceMap.get("default");
        } else {
            return dataSourceMap.get(context.getDatasourceName());
        }
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

package io.github.devlibx.easy.database.mysql;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.github.devlibx.easy.database.mysql.transaction.TransactionContext;
import io.github.devlibx.easy.database.mysql.transaction.TransactionContext.Context;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.devlibx.easy.database.DatabaseConstant.DATASOURCE_DEFAULT;

@Slf4j
public class DataSourceFactory {
    @Getter
    private final Map<String, DataSource> dataSourceMap;
    private final boolean transactionAwareDatasource;

    private boolean initialized = false;
    private final Object LOCK = new Object();

    @Inject
    public DataSourceFactory(@Named("enable-transaction-aware-datasource") boolean transactionAwareDatasource) {
        this.transactionAwareDatasource = transactionAwareDatasource;
        this.dataSourceMap = new HashMap<>();
    }

    public void register(DataSource dataSource) {

        // Try to install MetricRegistry if possible
        installMetricRegistry(dataSource);

        synchronized (LOCK) {
            if (transactionAwareDatasource) {
                dataSourceMap.put(DATASOURCE_DEFAULT, new TransactionAwareDataSourceProxy(dataSource));
            } else {
                dataSourceMap.put(DATASOURCE_DEFAULT, dataSource);
            }
            initialized = true;
        }
    }

    public void register(String dataSourceName, DataSource dataSource) {

        // Try to install MetricRegistry if possible
        installMetricRegistry(dataSource);

        synchronized (LOCK) {
            if (transactionAwareDatasource) {
                dataSourceMap.put(dataSourceName, new TransactionAwareDataSourceProxy(dataSource));
            } else {
                dataSourceMap.put(dataSourceName, dataSource);
            }
            initialized = true;
        }
    }

    private void ensureInitialization() {
        if (!initialized) {
            synchronized (LOCK) {
                if (!initialized) {
                    throw new RuntimeException("Datasource(s) are not initialized as of now. You must call IDatabaseService.startDatabase() before requesting for datasource");
                }
            }
        }
    }

    public DataSource getDataSource(String dataSourceName) {
        ensureInitialization();
        return dataSourceMap.get(dataSourceName);
    }

    public DataSource getDataSource() {
        ensureInitialization();
        Context context = TransactionContext.getInstance().getContext();
        if (context == null || Strings.isNullOrEmpty(context.getDatasourceName()) || Objects.equals(DATASOURCE_DEFAULT, context.getDatasourceName())) {
            return dataSourceMap.get(DATASOURCE_DEFAULT);
        } else {
            return dataSourceMap.get(context.getDatasourceName());
        }
    }

    public void shutdown() {
        int count = dataSourceMap.size();
        AtomicLong closed = new AtomicLong();
        dataSourceMap.forEach((name, dataSource) -> {
            log.info("Close Datasource Begin: {}", name);
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
                closed.incrementAndGet();
            } else if (dataSource instanceof TransactionAwareDataSourceProxy) {
                TransactionAwareDataSourceProxy proxy = (TransactionAwareDataSourceProxy) dataSource;
                DataSource underLyingDataSource = proxy.getTargetDataSource();
                if (underLyingDataSource instanceof HikariDataSource) {
                    ((HikariDataSource) underLyingDataSource).close();
                    closed.incrementAndGet();
                }
            }
        });
        dataSourceMap.clear();

        if (closed.get() != count) {
            throw new RuntimeException("We had " + count + " datasource registered, but only " + closed.get() + " are closed. This will cause connection leak. Please check.");
        }
    }

    private void installMetricRegistry(DataSource dataSource) {
        try {
            MetricRegistry metricRegistry = ApplicationContext.getInstance(MetricRegistry.class);
            if (metricRegistry == null) {
                log.warn("MetricRegistry is not found - we will not register metric for datasource");
                return;
            }
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource _dataSource = (HikariDataSource) dataSource;
                _dataSource.setMetricRegistry(metricRegistry);
                log.warn("MetricRegistry registered: dataSource={}, metricRegistry={}", _dataSource, metricRegistry);
            } else {
                log.warn("MetricRegistry is not found - we will not register metric for datasource (only HikariDataSource is supported)");
            }
        } catch (Throwable e) {
            log.warn("Failed to register metric for datasource: datasource={}", dataSource, e);
        }
    }
}

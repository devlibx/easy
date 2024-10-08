package io.github.devlibx.easy.database.mysql;

import io.github.devlibx.easy.database.IDatabaseService;
import io.github.devlibx.easy.database.mysql.config.MySqlConfigs;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;

@Slf4j
public class DatabaseService implements IDatabaseService {
    private final MySqlConfigs dbConfigs;
    private final DataSourceFactory dataSourceFactory;

    @Inject
    public DatabaseService(MySqlConfigs dbConfigs, DataSourceFactory dataSourceFactory) {
        this.dbConfigs = dbConfigs;
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void startDatabase() {
        if (dbConfigs == null || dbConfigs.getConfigs() == null || dbConfigs.getConfigs().isEmpty()) {
            throw new RuntimeException("MySqlConfigs is null or empty. " +
                    "(If using Guice) Please check if you forgot to call bind(MySqlConfigs.class).toInstance(yourConfigs)");
        }
        dbConfigs.getConfigs().forEach((name, mySqlConfig) -> {
            dataSourceFactory.register(name, mySqlConfig.buildHikariDataSource());
        });
    }

    @Override
    public void stopDatabase() {
        dataSourceFactory.shutdown();
    }
}

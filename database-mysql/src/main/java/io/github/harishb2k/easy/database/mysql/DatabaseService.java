package io.github.harishb2k.easy.database.mysql;

import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfigs;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

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
            throw new RuntimeException("MySqlConfigs is null or empty");
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

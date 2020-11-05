package io.gitbub.harishb2k.easy.helper.mysql;

import io.gitbub.harishb2k.easy.helper.Safe;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

@SuppressWarnings("rawtypes")
@Slf4j
public class DockerMySqTestHelper implements IMySqlTestHelper {
    private MySQLContainer container;
    private TestMySqlConfig config;

    @Override
    public void startMySql(TestMySqlConfig config) {
        this.config = config;
        container = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withDatabaseName(config.getDatabase())
                .withUsername(config.getUser())
                .withPassword(config.getPassword())
                .withEnv("MYSQL_ROOT_HOST", "%")
                .withExposedPorts(3306);
        try {
            container.start();
            this.config.setJdbcUrl(config.getJdbcUrl());
        } catch (ContainerLaunchException e) {
            log.error("Failed to start MySQLContainer - {}", e.getMessage());
        }
    }

    @Override
    public void stopMySql() {
        Safe.safe(() -> container.stop());
    }

    @Override
    public TestMySqlConfig getMySqlConfig() {
        return config;
    }

    @Override
    public boolean isMySqlRunning() {
        if (container == null) return false;
        return container.isHealthy();
    }
}

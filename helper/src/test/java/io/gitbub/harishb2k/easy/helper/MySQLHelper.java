package io.gitbub.harishb2k.easy.helper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

import java.util.Optional;

@SuppressWarnings("all")
@Slf4j
public class MySQLHelper {
    @Getter
    private String jdbcUrl = "jdbc:mysql://localhost:3306/users?useSSL=false";

    @Getter
    private String userName = "test";

    @Getter
    private String password = "test";


    private MySQLContainer container;
    private IMySQLHelperPlugin mySQLHelperPlugin;

    public String getJdbcUrl() {
        if (container != null) {
            return container.getJdbcUrl();
        } else if (mySQLHelperPlugin != null) {
            return mySQLHelperPlugin.getJdbcUrl();
        }
        return "";
    }

    public String getUserName() {
        if (container != null) {
            return "test";
        } else if (mySQLHelperPlugin != null) {
            return mySQLHelperPlugin.getUserName();
        }
        return "";
    }

    public String getPassword() {
        if (container != null) {
            return "test";
        } else if (mySQLHelperPlugin != null) {
            return mySQLHelperPlugin.getPassword();
        }
        return "";
    }

    public boolean canRunMySQL() {
        if (container != null) {
            return true;
        } else if (mySQLHelperPlugin != null) {
            return mySQLHelperPlugin.canRunMySQL();
        }
        return container != null;
    }

    public boolean startMySQL(Optional<IMySQLHelperPlugin> optionalPlugin) throws RuntimeException {

        if (optionalPlugin.isPresent()) {
            mySQLHelperPlugin = optionalPlugin.get();
            return mySQLHelperPlugin.startMySQL(jdbcUrl, userName, password);
        }

        container = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withEnv("MYSQL_ROOT_HOST", "%")
                .withExposedPorts(3306);
        try {
            container.start();
            return true;
        } catch (ContainerLaunchException e) {
            log.error("Failed to start MySQLContainer - {}", e.getMessage());
        }
        return false;
    }

    public void stopMySQL() {
        Safe.safe(() -> {
            container.stop();
        });
    }

    public interface IMySQLHelperPlugin {
        boolean startMySQL(String defaultJdbcUrl, String userName, String password);

        boolean canRunMySQL();

        String getJdbcUrl();

        String getUserName();

        String getPassword();
    }
}

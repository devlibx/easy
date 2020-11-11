package io.github.harishb2k.easy.testing.mysql;

import ch.qos.logback.classic.Level;
import com.zaxxer.hikari.HikariDataSource;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.Safe;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.shaded.com.github.dockerjava.core.command.AbstrDockerCmd;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"rawtypes", "unchecked"})
@Slf4j
public class MySqlExtension implements ParameterResolver, AfterEachCallback, BeforeEachCallback {
    public static final String DEFAULT_DATASOURCE_NAME = "default";
    public static final String DATASOURCE_NAME_PREFIX = "dataSourceName";

    private Map<String, DockerMySqlHolder> dockerMySqlHolderMap;
    private Map<String, LocalMySqlHolder> localMySqlHolderMap;
    private String database;
    private String username;
    private String password;
    private String host;
    private int port;
    private String name;

    private String findDataSourceByTagName(ExtensionContext context) {
        // Find the datasource name
        AtomicReference<String> name = new AtomicReference<>(DEFAULT_DATASOURCE_NAME);
        context.getTags().forEach(tag -> {
            if (tag.startsWith(DATASOURCE_NAME_PREFIX + "=")) {
                StringTokenizer st = new StringTokenizer(tag, "=");
                st.nextToken();
                name.set(st.nextToken());
            }
        });
        return name.get();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DockerClientProviderStrategy.class).setLevel(Level.OFF);
        LoggingHelper.getLogger(AbstrDockerCmd.class).setLevel(Level.OFF);

        // Make local and docker names
        final String name = this.name;// findDataSourceByTagName(context);
        final String dockerMySQLName = "docker_" + name;
        final String localMySQLName = "local_" + name;

        // MySQL config to run MySQL
        MySqlConfig config = new MySqlConfig();
        config.setHost(host);
        config.setPort(port);
        config.setDatabase(database);
        config.setUsername(username);
        config.setPassword(password);

        // Check global store to see if we already have kafka client
        ExtensionContext.Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);

        // Get the root map which stores MySql
        localMySqlHolderMap = (Map<String, LocalMySqlHolder>) store.getOrComputeIfAbsent(
                "__mysql_local_map__",
                _name -> new HashMap<String, LocalMySqlHolder>()
        );

        // Create docker mysql holder if possible
        dockerMySqlHolderMap = (Map<String, DockerMySqlHolder>) store.getOrComputeIfAbsent(
                "__mysql_docker_map__",
                _name -> new HashMap<String, DockerMySqlHolder>()
        );

        // Create local mysql holder if possible
        if (!localMySqlHolderMap.containsKey(localMySQLName)) {
            LocalMySqlHolder localMySqlHolder = new LocalMySqlHolder(config);
            localMySqlHolderMap.put(localMySQLName, localMySqlHolder);
            if (localMySqlHolder.isRunning()) {
                return;
            }
        } else if (localMySqlHolderMap.containsKey(localMySQLName)) {
            LocalMySqlHolder localMySqlHolder = localMySqlHolderMap.get(localMySQLName);
            if (localMySqlHolder.isRunning()) {
                return;
            }
        }

        if (!dockerMySqlHolderMap.containsKey(dockerMySQLName)) {
            DockerMySqlHolder dockerMySqlHolder = new DockerMySqlHolder(config);
            dockerMySqlHolderMap.put(dockerMySQLName, dockerMySqlHolder);
        } else if (dockerMySqlHolderMap.containsKey(dockerMySQLName)) {
            DockerMySqlHolder dockerMySqlHolder = dockerMySqlHolderMap.get(dockerMySQLName);
            if (!dockerMySqlHolder.isRunning()) {
                if (dockerMySqlHolder.mySqlContainerRunnable) {
                    dockerMySqlHolder.start(config);
                }
            }
        }
    }

    /**
     * Make a MySqlExtension with name
     */
    public static MySqlExtensionBuilder builder(String name) {
        return new MySqlExtensionBuilder(name);
    }

    /**
     * Make a MySqlExtension with name=default
     */
    public static MySqlExtensionBuilder builder() {
        return new MySqlExtensionBuilder(DEFAULT_DATASOURCE_NAME);
    }

    private String findDataSourceNameFromMethodParameterAnnotation(ParameterContext parameterContext) {
        MySqlDataSource dataSourceAnnotation = parameterContext.findAnnotation(MySqlDataSource.class).orElse(null);
        String datasourceName = DEFAULT_DATASOURCE_NAME;
        if (dataSourceAnnotation != null) {
            datasourceName = dataSourceAnnotation.value();
        }
        return datasourceName;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        boolean match = parameterContext.getParameter().getType() == MySqlConfig.class
                || parameterContext.getParameter().getType() == DataSource.class;
        if (match) {
            String name = findDataSourceNameFromMethodParameterAnnotation(parameterContext);
            return Objects.equals(name, this.name);
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        String datasourceName = findDataSourceNameFromMethodParameterAnnotation(parameterContext);
        if (!Objects.equals(datasourceName, name)) return null;

        if (parameterContext.getParameter().getType() == MySqlConfig.class) {
            MySqlConfig config = new MySqlConfig();
            config.setRunning(isMySqlRunning(datasourceName));
            if (config.isRunning()) {
                config.setJdbcUrl(getJdbcUrl(datasourceName));
                config.setUsername(username);
                config.setPassword(password);
            }
            return config;
        } else if (parameterContext.getParameter().getType() == DataSource.class) {
            if (isMySqlRunning(datasourceName)) {
                HikariDataSource dataSource = new HikariDataSource();
                dataSource.setJdbcUrl(getJdbcUrl(datasourceName));
                dataSource.setDriverClassName("com.mysql.jdbc.Driver");
                dataSource.setUsername(username);
                dataSource.setPassword(password);
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put("datasource", dataSource);
                return dataSource;
            }
        }
        return null;
    }

    private boolean isMySqlRunning(String dataSourceName) {
        if (localMySqlHolderMap.containsKey("local_" + dataSourceName)) {
            if (localMySqlHolderMap.get("local_" + dataSourceName).isRunning()) {
                return true;
            }
        }
        if (dockerMySqlHolderMap.containsKey("docker_" + dataSourceName)) {
            if (dockerMySqlHolderMap.get("docker_" + dataSourceName).isRunning()) {
                return true;
            }
        }
        return false;
    }

    private String getJdbcUrl(String dataSourceName) {
        if (localMySqlHolderMap.containsKey("local_" + dataSourceName)) {
            if (localMySqlHolderMap.get("local_" + dataSourceName).isRunning()) {
                return localMySqlHolderMap.get("local_" + dataSourceName).jdbcUrl;
            }
        }
        if (dockerMySqlHolderMap.containsKey("docker_" + dataSourceName)) {
            if (dockerMySqlHolderMap.get("docker_" + dataSourceName).isRunning()) {
                return dockerMySqlHolderMap.get("docker_" + dataSourceName).mySQLContainer.getJdbcUrl();
            }
        }
        return null;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Safe.safe(() -> {
            HikariDataSource dataSource = (HikariDataSource) context.getStore(ExtensionContext.Namespace.GLOBAL).get("datasource");
            if (dataSource != null) {
                dataSource.close();
            }
        });
    }

    public static class MySqlExtensionBuilder {
        private final String name;
        private String database;
        private String username;
        private String password;
        private String host = "localhost";
        private int port = 3306;

        public MySqlExtensionBuilder(String name) {
            this.name = name;
        }

        public MySqlExtension build() {
            MySqlExtension extension = new MySqlExtension();
            extension.database = database;
            extension.username = username;
            extension.password = password;
            extension.host = host;
            extension.port = port;
            extension.name = name;
            return extension;
        }

        public MySqlExtensionBuilder withDatabase(String database) {
            this.database = database;
            return this;
        }

        public MySqlExtensionBuilder withHost(String host) {
            this.host = host;
            this.port = 3306;
            return this;
        }

        public MySqlExtensionBuilder withHostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
            return this;
        }

        public MySqlExtensionBuilder withUsernamePassword(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }
    }

    private static class LocalMySqlHolder {
        private final MySqlConfig config;
        private final HikariDataSource dataSource;
        private final String jdbcUrl;

        public LocalMySqlHolder(MySqlConfig config) {
            this.config = config;
            this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false", config.getHost(), config.getPort(), config.getDatabase());
            this.dataSource = getDatasource();
        }

        public boolean isRunning() {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1;")) {
                ResultSet resultset = statement.executeQuery();
                return resultset.next();
            } catch (Exception ignored) {
            }
            return false;
        }

        public void close() {
            Safe.safe(() -> {
                if (dataSource != null) {
                    dataSource.close();
                }
            });
        }

        private HikariDataSource getDatasource() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUsername(config.getUsername());
            dataSource.setPassword(config.getPassword());
            return dataSource;
        }
    }

    @SuppressWarnings({"FieldCanBeLocal", "rawtypes"})
    private static class DockerMySqlHolder {
        private MySQLContainer mySQLContainer;
        public boolean mySqlContainerRunnable = false;

        public DockerMySqlHolder(MySqlConfig config) {
            start(config);
        }

        private void start(MySqlConfig config) {
            try {
                log.info("Try to create a client for docker mysql - to see if we can use docker mysql");
                mySQLContainer = (MySQLContainer) new MySQLContainer("mysql:5.5")
                        .withDatabaseName(config.getDatabase())
                        .withUsername(config.getUsername())
                        .withPassword(config.getPassword())
                        .withEnv("MYSQL_ROOT_HOST", "%")
                        .withExposedPorts(3306);
                mySQLContainer.start();
                mySqlContainerRunnable = true;
                log.info("docker mysql available");
            } catch (Exception e) {
                log.error("failed to start docker mysql: error={}", e.getMessage());
                mySQLContainer = null;
            }
        }

        public boolean isRunning() {
            if (mySQLContainer != null) {
                return mySQLContainer.isRunning();
            }
            return false;
        }
    }
}

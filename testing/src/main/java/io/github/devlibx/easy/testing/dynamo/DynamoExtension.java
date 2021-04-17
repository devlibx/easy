package io.github.devlibx.easy.testing.dynamo;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import io.github.devlibx.easy.testing.mysql.MySqlExtension;
import io.github.devlibx.easy.testing.mysql.TestingDynamoDbDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;
import org.testcontainers.dynamodb.DynaliteContainer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
@Slf4j
public class DynamoExtension implements ParameterResolver, AfterEachCallback, BeforeEachCallback {
    private Map<String, DockerDynamoHolder> dockerDynamoDbHolderMap;
    private final String name;

    private DynamoExtension(String name) {
        this.name = name;
    }

    /**
     * Make a MySqlExtension with name
     */
    public static DynamoExtension builder(String name) {
        return new DynamoExtension(name);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {

        final String name = this.name;
        final String dockerDynamoLName = "docker_" + name;

        // Check global store to see if we already have kafka client
        ExtensionContext.Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);


        // Create docker mysql holder if possible
        dockerDynamoDbHolderMap = (Map<String, DockerDynamoHolder>) store.getOrComputeIfAbsent(
                "__dynamo_docker_map__",
                _name -> new HashMap<String, DockerDynamoHolder>()
        );

        if (!dockerDynamoDbHolderMap.containsKey(dockerDynamoLName)) {
            DockerDynamoHolder dockerMySqlHolder = new DockerDynamoHolder();
            dockerDynamoDbHolderMap.put(dockerDynamoLName, dockerMySqlHolder);
        } else if (dockerDynamoDbHolderMap.containsKey(dockerDynamoLName)) {
            DockerDynamoHolder dockerMySqlHolder = dockerDynamoDbHolderMap.get(dockerDynamoLName);
            if (!dockerMySqlHolder.isRunning()) {
                if (dockerMySqlHolder.dynamoDbContainerRunnable) {
                    dockerMySqlHolder.start();
                }
            }
        }
    }

    private String findDataSourceNameFromMethodParameterAnnotation(ParameterContext parameterContext) {
        TestingDynamoDbDataSource dataSourceAnnotation = parameterContext.findAnnotation(TestingDynamoDbDataSource.class).orElse(null);
        String datasourceName = MySqlExtension.DEFAULT_DATASOURCE_NAME;
        if (dataSourceAnnotation != null) {
            datasourceName = dataSourceAnnotation.value();
        }
        return datasourceName;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        boolean match = parameterContext.getParameter().getType() == TestingDynamoDbConfig.class
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


        if (parameterContext.getParameter().getType() == TestingDynamoDbConfig.class) {
            TestingDynamoDbConfig config = new TestingDynamoDbConfig();
            config.setEndpointConfiguration(getEndPoint(datasourceName));
            return config;
        }

        return null;
    }

    private EndpointConfiguration getEndPoint(String dataSourceName) {
        if (dockerDynamoDbHolderMap.containsKey("docker_" + dataSourceName)) {
            if (dockerDynamoDbHolderMap.get("docker_" + dataSourceName).isRunning()) {
                return dockerDynamoDbHolderMap.get("docker_" + dataSourceName).dynamoContainer.getEndpointConfiguration();
            }
        }
        return null;
    }

    private boolean isRunning(String dataSourceName) {
        if (dockerDynamoDbHolderMap.containsKey("docker_" + dataSourceName)) {
            if (dockerDynamoDbHolderMap.get("docker_" + dataSourceName).isRunning()) {
                return true;
            }
        }
        return false;
    }

    private static class DockerDynamoHolder {
        private DynaliteContainer dynamoContainer;
        public boolean dynamoDbContainerRunnable = false;

        public DockerDynamoHolder() {
            start();
        }

        private void start() {
            try {
                log.info("Try to create a client for docker dynamo - to see if we can use docker mysql");
                // dynamoContainer = new DynaliteContainer("amazon/dynamodb-local:latest");
                dynamoContainer = new DynaliteContainer();
                dynamoContainer.start();
                dynamoDbContainerRunnable = true;
                log.info("docker dynamodb available");
            } catch (Exception e) {
                log.error("failed to start docker dynamodb: error={}", e.getMessage());
                dynamoContainer = null;
                e.printStackTrace();
            }
        }

        public boolean isRunning() {
            if (dynamoContainer != null) {
                return dynamoContainer.isRunning();
            }
            return false;
        }
    }
}

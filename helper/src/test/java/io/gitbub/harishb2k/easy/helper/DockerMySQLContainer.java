package io.gitbub.harishb2k.easy.helper;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

@SuppressWarnings("rawtypes")
public class DockerMySQLContainer {
    private static String jdbcUrl = "jdbc:mysql://localhost:3306/users?useSSL=false";
    private static MySQLContainer container;

    public static void startMySQL() throws RuntimeException {
        container = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withEnv("MYSQL_ROOT_HOST", "%")
                .withExposedPorts(3306);
        try {
            container.start();
        } catch (ContainerLaunchException e) {
            throw new RuntimeException(e);
        }
    }
}

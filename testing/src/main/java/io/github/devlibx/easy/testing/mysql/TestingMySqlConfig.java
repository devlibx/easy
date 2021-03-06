package io.github.devlibx.easy.testing.mysql;

import lombok.Data;

@Data
public class TestingMySqlConfig {
    private String name;
    private boolean running;

    private String jdbcUrl;

    private String database;
    private String username;
    private String password;
    private String host;
    private int port;

    String uniqueName() {
        return String.format("%s-%s-%s", database, username, password);
    }
}

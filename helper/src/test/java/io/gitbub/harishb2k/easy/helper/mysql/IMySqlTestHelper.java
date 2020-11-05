package io.gitbub.harishb2k.easy.helper.mysql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface IMySqlTestHelper {
    String DEFAULT_TEST_DATABASE_NAME = "users";
    String DEFAULT_TEST_DATABASE_URL = "jdbc:mysql://localhost:3306/" + DEFAULT_TEST_DATABASE_NAME + "?useSSL=false";
    String DEFAULT_TEST_DATABASE_USER = "test";
    String DEFAULT_TEST_DATABASE_PASSWORD = "test";

    default void installCustomMySqlTestHelper(IMySqlTestHelper customMySqlTestHelper) {
    }

    void startMySql(TestMySqlConfig config);

    void stopMySql();

    TestMySqlConfig getMySqlConfig();

    boolean isMySqlRunning();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    class TestMySqlConfig {
        private String jdbcUrl;
        private String database;
        private String user;
        private String password;

        public static TestMySqlConfig withDefaults() {
            return TestMySqlConfig.builder()
                    .jdbcUrl(DEFAULT_TEST_DATABASE_URL)
                    .database(DEFAULT_TEST_DATABASE_NAME)
                    .user(DEFAULT_TEST_DATABASE_USER)
                    .password(DEFAULT_TEST_DATABASE_PASSWORD)
                    .build();
        }
    }

    class MySqlNotRunningException extends RuntimeException {
        MySqlNotRunningException(Throwable e) {
            super("Failed to find a running mysql", e);
        }
    }
}

package io.github.devlibx.easy;

import io.github.devlibx.easy.testing.kafka.TestingKafkaConfig;
import io.github.devlibx.easy.testing.kafka.KafkaExtension;
import io.github.devlibx.easy.testing.mysql.TestingMySqlConfig;
import io.github.devlibx.easy.testing.mysql.MySqlExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;

@ExtendWith(KafkaExtension.class)
public class MySqlAndKafkaExtensionTest {

    @RegisterExtension
    public static MySqlExtension mysql = MySqlExtension.builder()
            .withDatabase("users")
            .withUsernamePassword("test", "test")
            .build();

    @Test
    public void testMySQL(TestingMySqlConfig mySqlConfig, DataSource dataSource, TestingKafkaConfig kafkaConfig) {
        Assumptions.assumeTrue(mySqlConfig.isRunning());
        Assumptions.assumeTrue(kafkaConfig.isRunning());
        Assertions.assertNotNull(dataSource);
    }
}

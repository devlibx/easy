package io.github.harishb2k.easy;

import io.github.harishb2k.easy.testing.kafka.KafkaConfig;
import io.github.harishb2k.easy.testing.kafka.KafkaExtension;
import io.github.harishb2k.easy.testing.mysql.MySqlConfig;
import io.github.harishb2k.easy.testing.mysql.MySqlExtension;
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
    public void testMySQL(MySqlConfig mySqlConfig, DataSource dataSource, KafkaConfig kafkaConfig) {
        Assumptions.assumeTrue(mySqlConfig.isRunning());
        Assumptions.assumeTrue(kafkaConfig.isRunning());
        Assertions.assertNotNull(dataSource);
    }
}

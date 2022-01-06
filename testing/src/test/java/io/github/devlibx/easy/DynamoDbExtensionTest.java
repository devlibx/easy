package io.github.devlibx.easy;

import io.github.devlibx.easy.testing.dynamo.DynamoExtension;
import io.github.devlibx.easy.testing.dynamo.TestingDynamoDbConfig;
import io.github.devlibx.easy.testing.mysql.MySqlExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DynamoDbExtensionTest {

    @RegisterExtension
    public static DynamoExtension defaultDynamoDb = DynamoExtension.builder(MySqlExtension.DEFAULT_DATASOURCE_NAME);

    @Test
    @DisplayName("Default dynamodb extension with default settings")
    @Disabled
    public void testDefaultDynamoDB(TestingDynamoDbConfig dynamoDbConfig) {
        Assumptions.assumeTrue(dynamoDbConfig != null);
        Assertions.assertNotNull(dynamoDbConfig.getEndpointConfiguration());
        Assertions.assertNotNull(dynamoDbConfig.getEndpointConfiguration().getServiceEndpoint());
    }
}

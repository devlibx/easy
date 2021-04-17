package io.github.devlibx.easy.testing.dynamo;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import lombok.Data;

@Data
public class TestingDynamoDbConfig {
    private EndpointConfiguration endpointConfiguration;
}

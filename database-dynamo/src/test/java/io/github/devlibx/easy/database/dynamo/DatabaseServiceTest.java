package io.github.devlibx.easy.database.dynamo;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.devlibx.easy.database.IDatabaseService;
import io.github.devlibx.easy.database.dynamo.config.DynamoConfig;
import io.github.devlibx.easy.database.dynamo.config.DynamoConfigs;
import io.github.devlibx.easy.database.dynamo.module.DatabaseDynamoModule;
import io.github.devlibx.easy.database.dynamo.operation.Attribute;
import io.github.devlibx.easy.database.dynamo.operation.Put;
import io.github.devlibx.easy.testing.dynamo.DynamoExtension;
import io.github.devlibx.easy.testing.dynamo.TestingDynamoDbConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class DatabaseServiceTest {
    @RegisterExtension
    public static DynamoExtension defaultDynamoDb = DynamoExtension.builder("default");

    @Disabled
    @Test
    public void runService(TestingDynamoDbConfig dynamoDbConfig) {
        Assertions.assertNotNull(dynamoDbConfig.getEndpointConfiguration());
        Assertions.assertNotNull(dynamoDbConfig.getEndpointConfiguration().getServiceEndpoint());

        DynamoConfig dynamoConfig = new DynamoConfig();
        dynamoConfig.setEndPoint(dynamoDbConfig.getEndpointConfiguration().getServiceEndpoint());
        dynamoConfig.setRegion(dynamoDbConfig.getEndpointConfiguration().getSigningRegion());

        DynamoConfigs configs = new DynamoConfigs();
        configs.addConfig(dynamoConfig);

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(DynamoConfigs.class).toInstance(configs);
            }
        }, new DatabaseDynamoModule());

        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();
        IDynamoHelper helper = injector.getInstance(IDynamoHelper.class);

        // Create Table
        String tableName = "devlibx_demo_app";
        CreateTableRequest request = new CreateTableRequest()
                .withAttributeDefinitions(
                        new AttributeDefinition("entityId", ScalarAttributeType.S),
                        new AttributeDefinition("namespace", ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement("entityId", KeyType.HASH),
                        new KeySchemaElement("namespace", KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withTableName(tableName);

        helper.execute(tableName, (client, table) -> {
            try {
                client.createTable(request);
            } catch (AmazonServiceException e) {
                System.err.println(e.getErrorMessage());
            }
        });

        Map<String, Object> international_travel_affinity_1 = new HashMap<>();
        international_travel_affinity_1.put("value", 5);
        international_travel_affinity_1.put("confidence", 0.8);


        String userId = UUID.randomUUID().toString();
        io.github.devlibx.easy.database.dynamo.operation.Put put = new Put();
        put = put
                .withTable(tableName)
                .withKey("entityId", "cred:user:" + userId)
                .withSortKey("namespace", "devlibx")
                .addAttribute(Attribute.builder().name("attr_1").value(international_travel_affinity_1).build())
                .addAttribute(Attribute.builder().name("attr_2").value(11).build())
        ;
        helper.persist(put);
        System.out.println("Result = " + userId + " table=" + tableName);
    }
}
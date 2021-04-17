package io.github.devlibx.easy.database.dynamo.demo;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.database.IDatabaseService;
import io.github.devlibx.easy.database.dynamo.IDynamoHelper;
import io.github.devlibx.easy.database.dynamo.config.DynamoConfig;
import io.github.devlibx.easy.database.dynamo.config.DynamoConfigs;
import io.github.devlibx.easy.database.dynamo.module.DatabaseDynamoModule;
import io.github.devlibx.easy.database.dynamo.operation.Get;
import io.github.devlibx.easy.database.dynamo.operation.Put;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class DemoApplication {

    // Demo table to create
    private final String TABLE_NAME = "dynamo_demo_app";

    public static void main(String[] args) {
        DemoApplication application = new DemoApplication();
        application.setup();

        System.out.println("Create a table...");
        application.createTable();

        System.out.println("Put a record in db..");
        StringObjectMap response = application.addItemToTable();

        System.out.println("Get a record in db..");
        ClientObject co = application.getItemFromTable(response);

        System.out.println("Result: " + co);
    }

    private void setup() {
        DynamoConfig dynamoConfig = new DynamoConfig();
        // TODO - dynamoConfig.setEndPoint("PUT YOUR ENDPOINT HERE");

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();

                DynamoConfigs configs = new DynamoConfigs();
                configs.addConfig(dynamoConfig);
                bind(DynamoConfigs.class).toInstance(configs);

            }
        }, new DatabaseDynamoModule());
        ApplicationContext.setInjector(injector);

        // Start DB (Mandatory step)
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();
    }

    // Create Table for testing
    // We just log a error if table exists
    private void createTable() {
        IDynamoHelper helper = ApplicationContext.getInstance(IDynamoHelper.class);

        CreateTableRequest request = new CreateTableRequest()
                .withAttributeDefinitions(
                        new AttributeDefinition("id", ScalarAttributeType.S),
                        new AttributeDefinition("scope", ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement("id", KeyType.HASH),
                        new KeySchemaElement("scope", KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withTableName(TABLE_NAME);

        // Helper gives a execute function which gives DynamoClient to work with
        helper.execute(TABLE_NAME, (client, table) -> {
            try {
                client.createTable(request);
            } catch (AmazonServiceException e) {
                System.err.println("***********************************");
                System.err.println(e.getErrorMessage());
                System.err.println("***********************************");
            }
        });
    }

    // Add a dummy item in our table
    public StringObjectMap addItemToTable() {
        IDynamoHelper helper = ApplicationContext.getInstance(IDynamoHelper.class);

        Map<String, Object> someAttribute = new HashMap<>();
        someAttribute.put("value", 5);
        someAttribute.put("score", 0.8);

        // Helper function which gives us a method to persist
        Put put = Put.builder(TABLE_NAME)
                .withKey("id", "d:user:" + UUID.randomUUID().toString(), "scope", "client")
                .addAttribute("attr_1", someAttribute)
                .addAttribute("attr_2", 1)
                .build();
        helper.persist(put);

        StringObjectMap response = new StringObjectMap();
        response.put("id", put.getKeyValue(), "scope", put.getSortKeyValue());
        return response;
    }

    // Helper to read a record
    public ClientObject getItemFromTable(StringObjectMap response) {
        IDynamoHelper helper = ApplicationContext.getInstance(IDynamoHelper.class);

        // Find a record from DB
        return helper.fineOne(
                Get.builder(TABLE_NAME)
                        .withKey("id", response.getString("id"))
                        .withSortKey("scope", response.getString("scope"))
                        .build(),
                new ICustomRowMapper(),
                ClientObject.class
        ).orElse(null);
    }

    // This is a  mapper function which can be implemented by client to convert DynamoDB object to POJO
    public static class ICustomRowMapper implements IDynamoHelper.IRowMapper<ClientObject> {

        @Override
        public ClientObject map(Item item) {
            ClientObject co = new ClientObject();
            co.setUserId(item.getString("id"));
            co.setNamespace(item.getString("scope"));
            item.attributes().forEach(stringObjectEntry -> {
                co.addAttribute(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            });
            return co;
        }
    }

    // Client specific POJO
    @Data
    public static class ClientObject {
        private String userId;
        private String namespace;
        private Map<String, Object> attributes;

        public void addAttribute(String key, Object value) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key, value);
        }
    }
}

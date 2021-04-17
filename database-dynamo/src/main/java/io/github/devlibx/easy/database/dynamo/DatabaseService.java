package io.github.devlibx.easy.database.dynamo;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import io.github.devlibx.easy.database.IDatabaseService;
import io.github.devlibx.easy.database.dynamo.config.DynamoConfigs;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class DatabaseService implements IDatabaseService {
    private final DynamoConfigs dbConfigs;
    private final DataSourceFactory dataSourceFactory;

    @Inject
    public DatabaseService(DynamoConfigs dynamoConfigs, DataSourceFactory dataSourceFactory) {
        this.dbConfigs = dynamoConfigs;
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void startDatabase() {
        if (dbConfigs == null || dbConfigs.getConfigs() == null || dbConfigs.getConfigs().isEmpty()) {
            throw new RuntimeException("DynamoConfigs is null or empty. " +
                    "(If using Guice) Please check if you forgot to call bind(DynamoConfigs.class).toInstance(yourConfigs)");
        }

        dbConfigs.getConfigs().forEach((name, dynamoConfig) -> {
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(dynamoConfig.getEndPoint(), dynamoConfig.getRegion()))
                    .build();
            DynamoDB dynamoDB = new DynamoDB(client);
            dataSourceFactory.add(name, dynamoDB);
        });
    }

    @Override
    public void stopDatabase() {
    }
}

package io.github.devlibx.easy.database.dynamo;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import io.github.devlibx.easy.database.dynamo.operation.Get;
import io.github.devlibx.easy.database.dynamo.operation.Put;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.devlibx.easy.database.DatabaseConstant.DATASOURCE_DEFAULT;

@SuppressWarnings("FuseStreamOperations")
public class DynamoHelper implements IDynamoHelper {
    private final DataSourceFactory dataSourceFactory;

    @Inject
    public DynamoHelper(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void persist(Put put) {
        DynamoDB client = dataSourceFactory.get(DATASOURCE_DEFAULT);
        Table table = client.getTable(put.getTable());

        List<AttributeUpdate> attributeUpdates = put.getAttributes().stream()
                .map(attribute -> {
                    AttributeUpdate update = new AttributeUpdate(attribute.getName());
                    update.put(attribute.getValue());
                    return update;
                }).collect(Collectors.toList());

        UpdateItemOutcome outcome = table.updateItem(
                put.getKeyName(), put.getKeyValue(),
                put.getSortKeyName(), put.getSortKeyValue(),
                attributeUpdates.toArray(new AttributeUpdate[0])
        );
    }

    @Override
    public <T> Optional<T> fineOne(Get get, IRowMapper<T> mapper, Class<T> cls) {
        DynamoDB client = dataSourceFactory.get(DATASOURCE_DEFAULT);
        Table table = client.getTable(get.getTable());
        GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey(get.getKeyName(), get.getKeyValue(), get.getSortKeyName(), get.getSortKeyValue())
                .withConsistentRead(get.isConsistentRead());
        Item item = table.getItem(spec);
        if (item != null) {
            return Optional.ofNullable(mapper.map(item));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void execute(String tableName, IDynamoOperation operation) {
        DynamoDB client = dataSourceFactory.get(DATASOURCE_DEFAULT);
        Table table = client.getTable(tableName);
        operation.process(client, table);
    }
}

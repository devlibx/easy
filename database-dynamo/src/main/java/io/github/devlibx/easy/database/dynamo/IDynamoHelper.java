package io.github.devlibx.easy.database.dynamo;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import io.github.devlibx.easy.database.dynamo.operation.Get;
import io.github.devlibx.easy.database.dynamo.operation.Put;

import java.util.Optional;

public interface IDynamoHelper {
    void persist(Put put);

    <T> Optional<T> fineOne(Get get, IRowMapper<T> mapper, Class<T> cls);

    void execute(String table, IDynamoOperation operation);

    interface IDynamoOperation {
        void process(DynamoDB client, Table table);
    }

    interface IRowMapper<T> {
        T map(Item item);
    }
}

package io.github.devlibx.easy.database.dynamo;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import io.github.devlibx.easy.database.dynamo.operation.Put;

public interface IDynamoHelper {
    void persist(Put put);


    void execute(String table, IDynamoOperation operation);

    interface IDynamoOperation {
        void process(DynamoDB client, Table table);
    }
}

package io.github.devlibx.easy.database.dynamo;

import io.github.devlibx.easy.database.dynamo.operation.Put;

public interface IDynamoHelper {
    void persist(Put put);
}

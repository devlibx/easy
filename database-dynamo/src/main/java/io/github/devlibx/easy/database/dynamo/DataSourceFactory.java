package io.github.devlibx.easy.database.dynamo;


import com.amazonaws.services.dynamodbv2.document.DynamoDB;

import java.util.HashMap;
import java.util.Map;

public class DataSourceFactory {
    private final Map<String, DynamoDB> clients;

    public DataSourceFactory() {
        this.clients = new HashMap<>();
    }

    public void add(String name, DynamoDB dynamoDB) {
        clients.put(name, dynamoDB);
    }

    public DynamoDB get(String db) {
        return clients.get(db);
    }
}

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

    /**
     * Example of a simple Mapper to convert from Dynamo Item to client POJO
     * <pre>
     *     // This is a  mapper function which can be implemented by client to convert DynamoDB object to POJO
     *     public static class ICustomRowMapper implements IRowMapper<ClientObject> {
     *
     *         @Override
     *         public ClientObject map(Item item) {
     *             ClientObject co = new ClientObject();
     *             co.setUserId(item.getString("id"));
     *             co.setNamespace(item.getString("scope"));
     *             item.attributes().forEach(stringObjectEntry -> {
     *                 co.addAttribute(stringObjectEntry.getKey(), stringObjectEntry.getValue());
     *             });
     *             return co;
     *         }
     *     }
     *
     *     // Client specific POJO
     *     @Data
     *     public static class ClientObject {
     *         private String userId;
     *         private String namespace;
     *         private Map<String, Object> attributes;
     *
     *         public void addAttribute(String key, Object value) {
     *             if (attributes == null) {
     *                 attributes = new HashMap<>();
     *             }
     *             attributes.put(key, value);
     *         }
     *     }
     * </pre>
     *
     * @param <T>
     */
    interface IRowMapper<T> {
        T map(Item item);
    }
}

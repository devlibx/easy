package io.github.devlibx.easy.database.dynamo.operation;

import lombok.Data;

import java.util.List;

@Data
public class Put {
    private String table;
    private String keyName;
    private Object keyValue;
    private String sortKeyName;
    private Object sortKeyValue;
    private List<Attribute> attributes;
}

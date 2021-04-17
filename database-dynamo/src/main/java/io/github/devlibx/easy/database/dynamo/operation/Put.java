package io.github.devlibx.easy.database.dynamo.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Put {
    private String table;
    private String keyName;
    private Object keyValue;
    private String sortKeyName;
    private Object sortKeyValue;
    private List<Attribute> attributes;

    public Put withTable(String table) {
        this.table = table;
        return this;
    }

    public Put withKey(String key, Object value) {
        this.keyName = key;
        this.keyValue = value;
        return this;
    }

    public Put withSortKey(String key, Object value) {
        this.sortKeyName = key;
        this.sortKeyValue = value;
        return this;
    }

    public Put addAttribute(Attribute attribute) {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        attributes.add(attribute);
        return this;
    }
}

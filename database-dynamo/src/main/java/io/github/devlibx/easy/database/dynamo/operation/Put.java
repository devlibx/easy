package io.github.devlibx.easy.database.dynamo.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of creating a Put item
 * <pre>
 *      Put put = Put.builder(TABLE_NAME)
 *                 .withKey("id", "d:user:" + UUID.randomUUID().toString(), "scope", "client")
 *                 .addAttribute("attr_1", someAttribute)
 *                 .addAttribute("attr_2", 1)
 *                 .build();
 * </pre>
 */
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

    public static Builder builder(String tableName) {
        return new Builder(tableName);
    }

    public static class Builder {
        private final String table;
        private String keyName;
        private Object keyValue;
        private String sortKeyName;
        private Object sortKeyValue;
        private List<Attribute> attributes;

        public Builder(String table) {
            this.table = table;
        }

        public Builder withKey(String keyName, Object keyValue) {
            this.keyName = keyName;
            this.keyValue = keyValue;
            return this;
        }

        public Builder withSortKey(String sortKeyName, Object sortKeyValue) {
            this.sortKeyName = sortKeyName;
            this.sortKeyValue = sortKeyValue;
            return this;
        }

        public Builder withKey(String keyName, Object keyValue, String sortKeyName, Object sortKeyValue) {
            this.keyName = keyName;
            this.keyValue = keyValue;
            this.sortKeyName = sortKeyName;
            this.sortKeyValue = sortKeyValue;
            return this;
        }

        public Builder addAttribute(String name, Object value) {
            if (attributes == null) {
                attributes = new ArrayList<>();
            }
            attributes.add(new Attribute(name, value));
            return this;
        }

        public Put build() {
            Put obj = new Put();
            obj.table = table;
            obj.keyName = keyName;
            obj.keyValue = keyValue;
            obj.sortKeyName = sortKeyName;
            obj.sortKeyValue = sortKeyValue;
            if (attributes != null) {
                obj.attributes = new ArrayList<>();
                obj.attributes.addAll(attributes);
            }
            return obj;
        }
    }
}

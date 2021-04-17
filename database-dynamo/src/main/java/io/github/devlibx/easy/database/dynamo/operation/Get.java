package io.github.devlibx.easy.database.dynamo.operation;

import lombok.Data;

@Data
public class Get {
    private String table;
    private String keyName;
    private Object keyValue;
    private String sortKeyName;
    private Object sortKeyValue;
    private boolean consistentRead = true;

    public static GetBuilder builder(String tableName) {
        return new GetBuilder(tableName);
    }

    public static class GetBuilder {
        private final String table;
        private String keyName;
        private Object keyValue;
        private String sortKeyName;
        private Object sortKeyValue;
        private boolean consistentRead = true;

        public GetBuilder(String table) {
            this.table = table;
        }

        public GetBuilder withConsistentRead(boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }


        public GetBuilder withKey(String keyName, Object keyValue) {
            this.keyName = keyName;
            this.keyValue = keyValue;
            return this;
        }

        public GetBuilder withSortKey(String sortKeyName, Object sortKeyValue) {
            this.sortKeyName = sortKeyName;
            this.sortKeyValue = sortKeyValue;
            return this;
        }

        public Get build() {
            Get get = new Get();
            get.table = table;
            get.keyName = keyName;
            get.keyValue = keyValue;
            get.sortKeyName = sortKeyName;
            get.sortKeyValue = sortKeyValue;
            get.consistentRead = consistentRead;
            return get;
        }
    }
}

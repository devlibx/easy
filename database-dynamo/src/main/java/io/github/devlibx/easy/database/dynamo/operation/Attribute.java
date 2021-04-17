package io.github.devlibx.easy.database.dynamo.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Attribute {
    private String name;
    private Object value;
}

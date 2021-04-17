package io.github.devlibx.easy.database.dynamo.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamoConfig {
    private String endPoint = "http://localhost:8000";
    private String region = "us-west-2";
}

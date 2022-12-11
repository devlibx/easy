package io.gitbub.devlibx.easy.helper.config.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KafkaConfig {

    private String name;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String host = "localhost";

    @Builder.Default
    private int port = 9092;

    @Builder.Default
    private StringObjectMap properties = new StringObjectMap();
}

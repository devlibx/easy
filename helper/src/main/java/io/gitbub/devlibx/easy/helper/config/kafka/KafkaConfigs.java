package io.gitbub.devlibx.easy.helper.config.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KafkaConfigs {

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Map<String, KafkaConfig> kafkaConfigs = new HashMap<>();

    @Builder.Default
    private StringObjectMap properties = new StringObjectMap();

    /**
     * Setup default
     */
    public void setupDefaults() {
        kafkaConfigs.forEach((name, kafkaConfig) -> {
            if (Strings.isNullOrEmpty(kafkaConfig.getName())) {
                kafkaConfig.setName(name);
            }
        });
    }

    /**
     * Get kafka config by name or empty.
     *
     * @param name unique name for redis config
     * @return Optional.empty() if it is not defined or if not disable, otherwise kafka config.
     */
    public Optional<KafkaConfig> getKafkaConfig(String name) {
        KafkaConfig kafkaConfig = null;
        if (kafkaConfigs != null && kafkaConfigs.containsKey(name)) {
            kafkaConfig = kafkaConfigs.get(name);
            if (kafkaConfig.isEnabled()) {
                return Optional.of(kafkaConfig);
            }
        }
        return Optional.empty();
    }
}

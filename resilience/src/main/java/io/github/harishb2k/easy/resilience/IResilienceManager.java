package io.github.harishb2k.easy.resilience;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface IResilienceManager {

    /**
     * @return get existing or a new processor to handle this request
     */
    IResilienceProcessor getOrCreate(ResilienceCallConfig config);

    /**
     * Configuration for ResilienceProcessor
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    class ResilienceCallConfig {
        private String id;
        private int concurrency = 10;
        private int timeout = 1000;
        private int queueSize = 100;
        private boolean useSemaphore = false;

        public static ResilienceCallConfigBuilder withDefaults() {
            return ResilienceCallConfig.builder()
                    .queueSize(100)
                    .timeout(1000)
                    .concurrency(10)
                    .useSemaphore(false);
        }
    }
}

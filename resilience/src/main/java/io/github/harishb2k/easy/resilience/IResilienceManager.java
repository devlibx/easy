package io.github.devlibx.easy.resilience;

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

        public static class ResilienceCallConfigBuilder {
            private String id;
            private int concurrency;
            private int timeout;
            private int queueSize;
            private boolean useSemaphore;

            ResilienceCallConfigBuilder() {
            }

            public ResilienceCallConfig.ResilienceCallConfigBuilder id(String id) {
                this.id = id;
                return this;
            }

            public ResilienceCallConfig.ResilienceCallConfigBuilder concurrency(int concurrency) {
                this.concurrency = concurrency;
                return this;
            }

            public ResilienceCallConfig.ResilienceCallConfigBuilder timeout(int timeout) {
                this.timeout = timeout;
                return this;
            }

            public ResilienceCallConfig.ResilienceCallConfigBuilder queueSize(int queueSize) {
                this.queueSize = queueSize;
                return this;
            }

            public ResilienceCallConfig.ResilienceCallConfigBuilder useSemaphore(boolean useSemaphore) {
                this.useSemaphore = useSemaphore;
                return this;
            }

            public ResilienceCallConfig build() {
                return new ResilienceCallConfig(this.id, this.concurrency, this.timeout, this.queueSize, this.useSemaphore);
            }

            public String toString() {
                return "ResilienceCallConfig.ResilienceCallConfigBuilder(id=" + this.id + ", concurrency=" + this.concurrency + ", timeout=" + this.timeout + ", queueSize=" + this.queueSize + ", useSemaphore=" + this.useSemaphore + ")";
            }
        }
    }
}

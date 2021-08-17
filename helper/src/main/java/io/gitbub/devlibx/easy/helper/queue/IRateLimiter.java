package io.gitbub.devlibx.easy.helper.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface IRateLimiter {
    void execute(Runnable runnable);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Config {
        public int waitTimeToAcquirePermissionToExecuteInMs;
        public int timeToResetPermissionBackToInitialValueInMs;
        public int limit;
    }
}

package io.gitbub.harishb2k.easy.helper.healthcheck;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public interface IHealthCheckProvider {

    Result check() throws Exception;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Result {
        private boolean healthy;
        private String message;
        private Throwable error;
        private Map<String, Object> details;
    }
}

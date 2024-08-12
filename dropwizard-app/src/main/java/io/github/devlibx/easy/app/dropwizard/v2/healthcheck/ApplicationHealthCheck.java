package io.github.devlibx.easy.app.dropwizard.v2.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.setup.Environment;
import io.gitbub.devlibx.easy.helper.healthcheck.IHealthCheckProvider;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ApplicationHealthCheck {
    private final Map<String, IHealthCheckProvider> summarizers;
    private final StringHelper stringHelper;

    @Inject
    public ApplicationHealthCheck(Map<String, IHealthCheckProvider> summarizers, StringHelper stringHelper) {
        this.summarizers = summarizers;
        this.stringHelper = stringHelper;
    }

    public void setupHealthChecks(Environment environment) {
        healthChecks().forEach(healthCheckHolder -> {
            environment.healthChecks().register(healthCheckHolder.name, healthCheckHolder.healthCheck);
        });
    }

    private List<HealthCheckHolder> healthChecks() {
        List<HealthCheckHolder> list = new ArrayList<>();
        summarizers.forEach((name, healthCheckProvider) -> {
            HealthCheck healthCheck = new HealthCheck() {
                @Override
                protected Result check() throws Exception {
                    IHealthCheckProvider.Result result = healthCheckProvider.check();

                    ResultBuilder rb = Result.builder();
                    if (result.getDetails() != null) {
                        rb.withDetail("details", result.getDetails());
                    }
                    if (result.isHealthy()) {
                        rb.healthy().withMessage(result.getMessage());
                    } else {
                        rb.unhealthy().withMessage(result.getMessage());
                    }
                    return rb.build();
                }
            };
            list.add(
                    HealthCheckHolder.builder()
                            .healthCheck(healthCheck)
                            .name(name)
                            .build()
            );
        });

        return list;
    }

    @Data
    @Getter
    @Builder
    public static class HealthCheckHolder {
        private String name;
        private HealthCheck healthCheck;
    }
}

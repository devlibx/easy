package io.github.devlibx.easy.app.dropwizard.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics.InvalidRegistryTypeFoundException;
import io.github.devlibx.easy.app.dropwizard.v2.proto.ProtobufBundle;
import io.github.devlibx.easy.metrics.prometheus.PrometheusMetrics;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseApplication<T extends Configuration> extends Application<T> {

    @Override
    public void initialize(Bootstrap<T> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        // Enabled protocol buffer support if it is enabled
        if (enableProtobufSupport()) {
            bootstrap.addBundle(new ProtobufBundle<>());
        }
    }

    @Override
    public void run(T t, Environment environment) throws Exception {
        environment.getObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    /**
     * Register prometheus. It will register only if we have setup correct dependencies.
     * <p>
     * NOTE - this method will not throw error, it will just log the error.
     * <p>
     * Dependency - We should have registered PrometheusMetrics as IMetrics.class
     **/
    protected void registerPrometheus(Environment environment) {
        IMetrics metrics = null;
        try {
            metrics = ApplicationContext.getInstance(IMetrics.class);
        } catch (Exception e) {
            log.error("Failed to registered prometheus - IMetrics instance is not found");
        }

        if (metrics instanceof PrometheusMetrics) {
            try {
                CollectorRegistry collectorRegistry = metrics.getRegistry(CollectorRegistry.class);
                collectorRegistry.register(new DropwizardExports(environment.metrics()));
                // environment.servlets().addServlet("prometheusMetrics", new MetricsServlet(collectorRegistry)).addMapping("/metrics");
            } catch (InvalidRegistryTypeFoundException e) {
                log.error("Failed to registered prometheus - registry type mus be CollectorRegistry");
            } catch (Exception e) {
                log.error("Failed to registered prometheus - unknown error", e);
            }
        } else {
            log.error("Failed to registered prometheus - IMetrics instance is of PrometheusMetrics type");
        }
    }

    /**
     * Support proto buffer support. By default it is disabled.
     *
     * @return if true then this dropwizard app will also support proto-buffer.
     */
    protected boolean enableProtobufSupport() {
        return false;
    }
}

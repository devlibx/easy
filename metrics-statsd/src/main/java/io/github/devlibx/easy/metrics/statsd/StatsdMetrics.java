package io.github.devlibx.easy.metrics.statsd;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.metrics.MetricsConfig;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class StatsdMetrics implements IMetrics {
    private final StatsDClient statsDClient;
    private final MetricsConfig metricsConfig;

    @Inject
    public StatsdMetrics(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
        if (!metricsConfig.isEnabled()) {
            statsDClient = new NoOpStatsDClient();
        } else {
            statsDClient = new NonBlockingStatsDClient(metricsConfig.getServiceName(), metricsConfig.getHost(), metricsConfig.getPort());
        }
    }

    @Override
    public <T> T getRegistry(Class<T> cls) {
        return null;
    }

    /*@Override
    public void observe(String name, double amt) {
        name = String.format("%s.%s", getPrefix(), name);
        statsDClient.recordExecutionTime(name, (long) amt);
    }*/

    @Override
    public void inc(String name, String... labels) {
        name = String.format("%s.%s", getPrefix(), name);
        String metricString = handleLabels(name, labels);
        statsDClient.count(metricString, 1L);
    }

    @Override
    public <T> T time(String name, Callable<T> callable, String... labels) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerCounter(String name, String help, String... labelNames) {
    }

    @Override
    public void registerTimer(String name, String help, String... labelNames) {
    }

    private String handleLabels(String name, String... labels) {
        if (labels == null || labels.length == 0 || labels.length % 2 != 0) {
            return name;
        }
        List<String> l = new ArrayList<>();
        l.add(name);
        for (int i = 0; i < labels.length; i = i + 2) {
            String k = labels[i];
            String v = labels[i + 1];
            l.add(k + "=" + v);
        }
        return String.join(",", l);
    }

    private String getPrefix() {
        return String.format("%s.metrics", metricsConfig.getEnv());
    }
}

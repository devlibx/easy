package io.github.devlibx.easy.metrics.statsd;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.metrics.MetricsConfig;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            statsDClient = new NonBlockingStatsDClient(
                    metricsConfig.getServiceName(),
                    metricsConfig.getHost(),
                    metricsConfig.getPort(),
                    metricsConfig.getProperties().getInt("buffer-size", 1_000_000)
            );
        }
    }

    @Override
    public <T> T getRegistry(Class<T> cls) {
        return null;
    }

    @Override
    public void observe(String name, double amt) {
        name = String.format("%s.%s", getPrefix(), name);
        statsDClient.recordExecutionTime(name, (long) amt);
    }

    @Override
    public void observe(String name, double amt, String... labels) {
        name = String.format("%s.%s", getPrefix(), name);
        String metricString = handleLabels(name, labels);
        statsDClient.recordExecutionTime(metricString, (long) amt);
    }

    @Override
    public void inc(String name, String... labels) {
        name = String.format("%s.%s", getPrefix(), name);
        String metricString = handleLabels(name, labels);
        statsDClient.count(metricString, 1L);
    }

    @Override
    public void inc(String name, long count, String... labels) {
        name = String.format("%s.%s", getPrefix(), name);
        String metricString = handleLabels(name, labels);
        statsDClient.count(metricString, count);
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

    String _handleLabels(String name, String... labels) {
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

    String handleLabels(String name, String... labels) {
        if (labels == null || labels.length == 0 || labels.length % 2 != 0) {
            return name;
        }
        List<String> keys = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < labels.length; i = i + 2) {
            keys.add(labels[i]);
            tags.put(labels[i], labels[i] + "=" + labels[i + 1]);
        }
        Collections.sort(keys);

        List<String> sortedList = new ArrayList<>();
        sortedList.add(name);
        for (String k : keys) {
            sortedList.add(tags.get(k));
        }
        return String.join(",", sortedList);
    }


    private String getPrefix() {
        return String.format("%s.metrics", metricsConfig.getEnv());
    }
}

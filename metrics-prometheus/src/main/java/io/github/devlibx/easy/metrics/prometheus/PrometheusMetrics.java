package io.github.devlibx.easy.metrics.prometheus;

import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@SuppressWarnings({"rawtypes", "unchecked", "FieldMayBeFinal"})
@Slf4j
public class PrometheusMetrics implements IMetrics {
    private final Map<String, SummaryHolder> summaryMap = new HashMap<>();
    private final Map<String, CounterHolder> counterMap = new HashMap<>();

    @Getter
    private final CollectorRegistry collectorRegistry = new CollectorRegistry();

    @Override
    public <T> T getRegistry(Class<T> cls) {
        if (cls.isAssignableFrom(CollectorRegistry.class)) {
            return (T) collectorRegistry;
        } else {
            throw new InvalidRegistryTypeFoundException("PrometheusMetrics uses io.prometheus.client.CollectorRegistry as registry class");
        }
    }

    @Override
    public void inc(String name, String... labels) {
        try {
            if (!counterMap.containsKey(name)) {
                registerCounter(name, name + " Help");
            }
            if (counterMap.get(name) != null) {
                counterMap.get(name).inc(labels);
            }
        } catch (RuntimeException e) {
            log.error("error in metrics inc method (runtime exception) - e={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("error in metrics inc method (exception) - e={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T time(String name, Callable<T> callable, String... labels) {
        try {
            // Just increase we do not have it registered already - register it
            if (!summaryMap.containsKey(name)) {
                registerTimer(name, name + " Help");
            }
            if (summaryMap.get(name) != null) {
                return (T) summaryMap.get(name).time(callable, labels);
            } else {
                return callable.call();
            }
        } catch (RuntimeException e) {
            log.error("error in timing method (runtime exception) - e={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("error in timing method (exception) - e={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void observe(String name, double amt) {
        try {
            // Just increase we do not have it registered already - register it
            if (!summaryMap.containsKey(name)) {
                registerTimer(name, name + " Help");
            }
            if (summaryMap.get(name) != null) {
                summaryMap.get(name).observe(amt);
            }
        } catch (RuntimeException e) {
            log.error("error in timing method (runtime exception) - e={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("error in timing method (exception) - e={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerCounter(String name, String help, String... labels) {
        try {
            Counter requests;
            if (labels != null && labels.length > 0) {
                requests = Counter.build().name(name).help(help).labelNames(labels).register();
            } else {
                requests = Counter.build().name(name).help(help).register();
            }
            collectorRegistry.register(requests);
            counterMap.put(name, new CounterHolder(name, requests, labels != null ? labels.length : 0));
        } catch (Exception e) {
            log.error("failed to register counter - name={}, error={}", name, e.getMessage());
        }
    }

    @Override
    public void registerTimer(String name, String help, String... labels) {
        try {
            Summary requests;
            if (labels != null && labels.length > 0) {
                requests = Summary.build().name(name).help(help).labelNames(labels)
                        .quantile(0.5, 0.05)
                        .quantile(0.9, 0.01)
                        .quantile(0.99, 0.001)
                        .register();
            } else {
                requests = Summary.build().name(name).help(help)
                        .quantile(0.5, 0.05)
                        .quantile(0.9, 0.01)
                        .quantile(0.99, 0.001)
                        .register();
            }
            collectorRegistry.register(requests);
            summaryMap.put(name, new SummaryHolder(name, requests, labels != null ? labels.length : 0));
        } catch (Exception e) {
            log.error("failed to register timer - name={}, error={}", name, e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    private static class CounterHolder {
        private String name;
        private Counter counter;
        private int labelCount;

        public void inc(String... labels) {
            if (labelCount == 0) {
                counter.inc();
            } else if (labels.length == labelCount) {
                counter.labels(labels).inc();
            } else {
                String[] temp = new String[labelCount];
                Arrays.fill(temp, "na");
                System.arraycopy(labels, 0, temp, 0, labels.length);
                counter.labels(temp).inc();
            }
        }
    }


    @Data
    @AllArgsConstructor
    private static class SummaryHolder {
        private String name;
        private Summary summary;
        private int labelCount;

        public Object time(Callable callable, String... labels) {
            if (labelCount == 0) {
                return summary.time(callable);
            } else if (labels.length == labelCount) {
                return summary.labels(labels).time(callable);
            } else {
                String[] temp = new String[labelCount];
                Arrays.fill(temp, "na");
                System.arraycopy(labels, 0, temp, 0, labels.length);
                return summary.labels(temp).time(callable);
            }
        }

        public void observe(double amt) {
            summary.observe(amt);
        }
    }
}

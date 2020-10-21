package io.github.harishb2k.easy.metrics.prometheus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics;
import io.gitbub.harishb2k.easy.helper.metrics.IMetrics.IMetricsLogger;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import junit.framework.TestCase;

import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

public class PrometheusMetricsTest extends TestCase {
    private final boolean defaultRegisterEnabled = true;
    private AtomicReference<String> ref;
    private IMetricsLogger metricsLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CollectorRegistry.defaultRegistry.clear();

        ref = new AtomicReference<>();
        metricsLogger = (format, args) -> {
            System.out.printf(format, args);
            ref.set(format);
        };
    }

    public void testCounterWithNoIMetricsLogger_NotRegistered() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        metrics.inc("dummy");

        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);
        if (defaultRegisterEnabled) {
            assertEquals(1.0, registry.getSampleValue("dummy"));
        } else {
            assertNull(registry.getSampleValue("dummy"));
        }
    }

    public void testCounterNotRegistered() {
        AtomicReference<String> ref = new AtomicReference<>();
        IMetricsLogger metricsLogger = (format, args) -> {
            System.out.printf(format, args);
            ref.set(format);
        };

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetricsLogger.class).toInstance(metricsLogger);
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);
        metrics.inc("dummy");

        if (defaultRegisterEnabled) {
            assertEquals(1.0, registry.getSampleValue("dummy"));
        } else {
            assertNotNull(ref.get());
            assertFalse(ref.get().isEmpty());
            assertNull(registry.getSampleValue("dummy"));
        }
    }

    public void testCounter() {
        AtomicReference<String> ref = new AtomicReference<>();
        IMetricsLogger metricsLogger = (format, args) -> {
            System.out.printf(format, args);
            ref.set(format);
        };

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetricsLogger.class).toInstance(metricsLogger);
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        metrics.registerCounter("dummy", "dummy help");
        metrics.inc("dummy");

        assertNull(ref.get());
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);
        assertEquals(1.0, registry.getSampleValue("dummy"));
    }

    public void testCounter_WithLabel() {
        AtomicReference<String> ref = new AtomicReference<>();
        IMetricsLogger metricsLogger = (format, args) -> {
            System.out.printf(format, args);
            ref.set(format);
        };

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetricsLogger.class).toInstance(metricsLogger);
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        metrics.registerCounter("dummy", "dummy help", "label_1", "label_2");
        metrics.inc("dummy");
        assertNull(ref.get());
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);
        Double value = registry.getSampleValue("dummy", new String[]{"label_1", "label_2"}, new String[]{"na", "na"});
        assertEquals(1.0, value);


        metrics.inc("dummy", "a");
        assertNull(ref.get());
        registry = metrics.getRegistry(CollectorRegistry.class);
        value = registry.getSampleValue("dummy", new String[]{"label_1", "label_2"}, new String[]{"a", "na"});
        assertEquals(1.0, value);

        metrics.inc("dummy", "a", "b");
        metrics.inc("dummy", "a", "b");
        metrics.inc("dummy", "a", "b");
        assertNull(ref.get());
        registry = metrics.getRegistry(CollectorRegistry.class);
        value = registry.getSampleValue("dummy", new String[]{"label_1", "label_2"}, new String[]{"a", "b"});
        assertEquals(3.0, value);
    }

    public void testSummary_NotRegistered_Without_MetricsLogger() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);

        // Test 1 => Call timer
        // Expected - we should not get any value
        metrics.time("dummy_timer", () -> "");
        assertNull(registry.getSampleValue("dummy_timer"));
    }

    public void testSummary_NotRegistered_With_MetricsLogger() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetricsLogger.class).toInstance(metricsLogger);
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);

        // Test 1 => Call timer
        // Expected - we should not get any value (we should've logged a error using metrics logger)
        metrics.time("dummy_timer", () -> "");
        assertNull(registry.getSampleValue("dummy_timer"));

        if (defaultRegisterEnabled) {
            assertEquals(1.0, registry.getSampleValue("dummy_timer_count"));
        } else {
            // Make sure we logger this error in metrics logger
            assertNotNull(ref.get());
            assertFalse(ref.get().isEmpty());
        }
    }

    public void testSummary_Registered_Without_MetricsLogger() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);

        // Test 1 => Call timer (register first)
        // Expected - we should not get any value (we should've logged a error using metrics logger)
        metrics.registerTimer("dummy_timer", "dummy_timer help");
        String result = metrics.time("dummy_timer", () -> "1234");
        assertEquals(1.0, registry.getSampleValue("dummy_timer_count"));
        assertEquals("1234", result);

        // Make sure we do not have any logging in  metrics logger
        assertNull(ref.get());
    }

    public void testSummary_Registered_Without_MetricsLogger_WithLabels() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
            }
        });
        IMetrics metrics = injector.getInstance(IMetrics.class);
        CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);

        // Test 1 => Call timer (register first)
        // Expected - we should not get any value (we should've logged a error using metrics logger)
        metrics.registerTimer("dummy_timer", "dummy_timer help", "l1", "l2");
        String result = metrics.time("dummy_timer", () -> "1234");
        assertEquals(1.0, registry.getSampleValue("dummy_timer_count", new String[]{"l1", "l2"}, new String[]{"na", "na"}));
        assertEquals("1234", result);

        result = metrics.time("dummy_timer", () -> "1234", "a");
        result = metrics.time("dummy_timer", () -> "1234", "a");
        assertEquals(2.0, registry.getSampleValue("dummy_timer_count", new String[]{"l1", "l2"}, new String[]{"a", "na"}));
        assertEquals("1234", result);

        result = metrics.time("dummy_timer", () -> "1234", "a", "b");
        result = metrics.time("dummy_timer", () -> "1234", "a", "b");
        result = metrics.time("dummy_timer", () -> "1234", "a", "c");
        result = metrics.time("dummy_timer", () -> "1234", "a", "c");
        assertEquals(2.0, registry.getSampleValue("dummy_timer_count", new String[]{"l1", "l2"}, new String[]{"a", "b"}));
        assertEquals(2.0, registry.getSampleValue("dummy_timer_count", new String[]{"l1", "l2"}, new String[]{"a", "c"}));
        assertEquals("1234", result);

        // Make sure we do not have any logging in  metrics logger
        assertNull(ref.get());

        Enumeration<Collector.MetricFamilySamples> samplesEnumeration = registry.metricFamilySamples();
        while (samplesEnumeration.hasMoreElements()) {
            Collector.MetricFamilySamples samples = samplesEnumeration.nextElement();
            System.out.println(samples.samples);
        }
    }
}
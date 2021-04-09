package io.github.devlibx.easy.metrics.prometheus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics.IMetricsLogger;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PrometheusMetricsTest {
    private final boolean defaultRegisterEnabled = true;
    private AtomicReference<String> ref;
    private IMetricsLogger metricsLogger;

    @BeforeEach
    protected void setUp() throws Exception {
        CollectorRegistry.defaultRegistry.clear();
        ref = new AtomicReference<>();
        metricsLogger = (format, args) -> {
            System.out.printf(format, args);
            ref.set(format);
        };
    }

    @Nested
    @DisplayName("Counter Tests")
    class CounterTests {
        @Test
        @DisplayName("Counter is increased without registration - self registration")
        public void counterIsIncreasedWithoutRegistration() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
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

        @Test
        @DisplayName("Counter is increased which was registered")
        public void registeredCounterIsIncreased() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
                }
            });
            IMetrics metrics = injector.getInstance(IMetrics.class);
            metrics.registerCounter("dummy", "dummy help");
            metrics.inc("dummy");

            CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);
            assertEquals(1.0, registry.getSampleValue("dummy"));
        }

        @Test
        @DisplayName("Counter is increased which was registered and has labels")
        public void registeredCounterWithLabelIsIncreased() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(IMetrics.class).to(PrometheusMetrics.class).in(Scopes.SINGLETON);
                }
            });
            IMetrics metrics = injector.getInstance(IMetrics.class);
            metrics.registerCounter("dummy", "dummy help", "label_1", "label_2");
            metrics.inc("dummy");
            CollectorRegistry registry = metrics.getRegistry(CollectorRegistry.class);
            Double value = registry.getSampleValue("dummy", new String[]{"label_1", "label_2"}, new String[]{"na", "na"});
            assertEquals(1.0, value);

            metrics.inc("dummy", "a");
            registry = metrics.getRegistry(CollectorRegistry.class);
            value = registry.getSampleValue("dummy", new String[]{"label_1", "label_2"}, new String[]{"a", "na"});
            assertEquals(1.0, value);

            metrics.inc("dummy", "a", "b");
            metrics.inc("dummy", "a", "b");
            metrics.inc("dummy", "a", "b");
            registry = metrics.getRegistry(CollectorRegistry.class);
            value = registry.getSampleValue("dummy", new String[]{"label_1", "label_2"}, new String[]{"a", "b"});
            assertEquals(3.0, value);
        }
    }


    @Nested
    @DisplayName("Counter Tests")
    class SummaryTests {
        @Test
        @DisplayName("Summary without registration should not output")
        public void summaryWithoutRegistrationShouldNotOutputMatrices() {
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

        @Test
        @DisplayName("Summary should output matrices when registered")
        public void summaryShouldOutputMatricesWhenRegistered() {
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
        }

        @Test
        @DisplayName("Summary should output matrices when registered with labels")
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

            Enumeration<Collector.MetricFamilySamples> samplesEnumeration = registry.metricFamilySamples();
            while (samplesEnumeration.hasMoreElements()) {
                Collector.MetricFamilySamples samples = samplesEnumeration.nextElement();
            }
        }
    }
}
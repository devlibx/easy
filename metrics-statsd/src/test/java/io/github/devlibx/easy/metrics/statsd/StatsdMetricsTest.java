package io.github.devlibx.easy.metrics.statsd;

import io.gitbub.devlibx.easy.helper.metrics.MetricsConfig;
import org.junit.Test;

public class StatsdMetricsTest {

    @Test
    public void testSendEventToStatsD() throws InterruptedException {
        String host = System.getenv("statsd");
        StatsdMetrics statsdMetrics = new StatsdMetrics(MetricsConfig.builder()
                .env("stage")
                .host(host)
                .port(80)
                .prefix("p")
                .serviceName("tests")
                .pushInterval(100)
                .enabled(true)
                .build()
        );
        for (int i = 0; i < 100; i++) {
            statsdMetrics.inc("sample_1", "city", "bangalore", "id", "10");
        }
        Thread.sleep(1000);
    }
}
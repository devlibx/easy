package io.github.devlibx.easy.metrics.statsd;

import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.MetricsConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

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
        Random random = new Random();
        for (int i = 0; i < 1_000; i++) {
            statsdMetrics.observe("sample_1_time", random.nextInt(50), "city", "bangalore", "id", "10");
            // Thread.sleep(1);
        }

        for (int i = 0; i < 1_000_000; i++) {
            statsdMetrics.gauge("sample_1_gauge", random.nextInt(50), "city", "bangalore", "id", "10");
             Thread.sleep(100);
        }

        Thread.sleep(10000);
    }

    @Test
    public void testCheckLabels() {
        StatsdMetrics statsdMetrics = new StatsdMetrics(MetricsConfig.builder().enabled(false).build());
        Assert.assertEquals("label_1", statsdMetrics.handleLabels("label_1"));
        Assert.assertEquals("label_1,tag1=value1,tag2=value2", statsdMetrics.handleLabels("label_1", "tag1", "value1", "tag2", "value2"));
        Assert.assertEquals("label_1,tag1=value1,tag2=value2", statsdMetrics.handleLabels("label_1", "tag2", "value2", "tag1", "value1"));
        Assert.assertEquals("sample_1_time,city=bangalore,id=10", statsdMetrics.handleLabels("sample_1_time", "city", "bangalore", "id", "10"));
        Assert.assertEquals("sample_1_time,city=bangalore,id=10", statsdMetrics.handleLabels("sample_1_time", "id", "10", "city", "bangalore"));
    }
}
package io.gitbub.devlibx.easy.helper.yaml;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.gitbub.devlibx.easy.helper.metrics.MetricsConfig;
import lombok.Data;
import org.junit.Assert;
import org.junit.Test;

public class YamlUtilsTest {

    @Test
    public void testReadYamlFromResourcePath() {
        TestClass config = YamlUtils.readYamlFromResourcePath("/test.yaml", TestClass.class);
        Assert.assertNotNull(config);
        Assert.assertEquals("harish", config.metricsConfig.getPrefix());
        Assert.assertFalse(config.metricsConfig.isEnabled());
    }

    @Data
    public static class TestClass {
        @JsonProperty("metricsConfig")
        private MetricsConfig metricsConfig;
    }

    @Test
    public void testJava19() {
        Thread.ofVirtual().start(new Runnable() {
            @Override
            public void run() {
                System.out.println("Java 19 working");
            }
        });
    }
}
package io.gitbub.devlibx.easy.helper.json;


import lombok.Data;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest {

    @Test
    public void asJson() {
        // We added Joda time and java.time.Instant support so making sure we do not fail
        TestPojo testPojo = new TestPojo();
        testPojo.setInstant(java.time.Instant.now());
        testPojo.setInstantJoda(DateTime.now());
        String json = JsonUtils.asJson(testPojo);
        System.out.println(json);
    }

    @Data
    public static class TestPojo {
        private java.time.Instant instant;
        private DateTime instantJoda;
    }
}
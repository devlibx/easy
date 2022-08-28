package io.github.devlibx.easy.rule.drools;


import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class DroolsHelperTest {

    @Test
    public void testDrools() throws Exception {
        DroolsHelper droolsHelper = new DroolsHelper();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("test-drools.drl").getFile());
        String filePath = file.getPath();
        droolsHelper.initialize(filePath);

        // Here we run your rule
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {

            // This is the result object we will populate
            ResultMap result = new ResultMap();

            // This is the input which we use to run rules
            StringObjectMap message = new StringObjectMap();
            message.put("data", StringObjectMap.of("order_status", "COMPLETED"));

            droolsHelper.execute("filter-input-stream", result, message);

            // Result will be populated with what we set in drools
            System.out.println(result);
        }

        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testDrools_OrderFlow_SkipTest() throws Exception {
        DroolsHelper droolsHelper = new DroolsHelper();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("test-timer-drools.drl").getFile());
        String filePath = file.getPath();
        droolsHelper.initialize(filePath);

        // Test 1 - Event should not skip
        ResultMap result = new ResultMap();
        StringObjectMap message = new StringObjectMap();
        message.put("data", StringObjectMap.of("order_status", "COMPLETED"));
        droolsHelper.execute("filter-input-stream", result, message);
        Assertions.assertFalse(result.getBoolean("skip"));

        // Test 2 - Event should skip
        result = new ResultMap();
        message = new StringObjectMap();
        message.put("data", StringObjectMap.of("order_status", "NOT_COMPLETED"));
        droolsHelper.execute("filter-input-stream", result, message);
        Assertions.assertTrue(result.getBoolean("skip"));
    }

    @Test
    public void testDrools_OrderFlow_StateTest() throws Exception {
        DroolsHelper droolsHelper = new DroolsHelper();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("test-timer-drools.drl").getFile());
        String filePath = file.getPath();
        droolsHelper.initialize(filePath);

        // Test 1 - Event should be stored in state
        ResultMap result = new ResultMap();
        StringObjectMap message = new StringObjectMap();
        message.put("data", StringObjectMap.of("order_status", "INIT", "order_id", "order_123", "user_id", "user_123"));
        droolsHelper.execute("initial-event-trigger", result, message);
        Assertions.assertTrue(result.getBoolean("retain-state"));
        Assertions.assertEquals(600, result.getInt("retain-ttl-in-sec"));
        Assertions.assertEquals(2, result.getInt("retain-state-expiry-in-sec"));
        Assertions.assertEquals("order_123", result.getString("retain-state-key"));
        Assertions.assertEquals("user_123", result.getStringObjectMap("retain-object").getString("user_id"));
        Assertions.assertEquals("order_123", result.getStringObjectMap("retain-object").getString("order_id"));

        // Test 2 - Event should be stored in state
        result = new ResultMap();
        message = new StringObjectMap();
        message.put("data", StringObjectMap.of("order_status", "COMPLETED", "order_id", "order_123", "user_id", "user_123"));
        droolsHelper.execute("initial-event-trigger", result, message);
        Assertions.assertFalse(result.getBoolean("retain-state"));
        Assertions.assertTrue(result.getBoolean("retain-state-delete"));
        Assertions.assertEquals("order_123", result.getString("retain-state-key"));
    }

    @Test
    public void testDrools_OrderFlow_TriggerExpiry() throws Exception {
        DroolsHelper droolsHelper = new DroolsHelper();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("test-timer-drools.drl").getFile());
        String filePath = file.getPath();
        droolsHelper.initialize(filePath);

        // Test 1 - Event should be stored in state
        ResultMap result = new ResultMap();
        StringObjectMap message = StringObjectMap.of("order_id", "order_123", "user_id", "user_123");
        droolsHelper.execute("expiry-event-trigger", result, message);

        Assertions.assertTrue(result.getBoolean("trigger-expiry"));
        System.out.println("Result:" + JsonUtils.asJson(result));
        Assertions.assertEquals("user_123", result.getStringObjectMap("trigger-object").getString("user_id"));
        Assertions.assertEquals("order_123", result.getStringObjectMap("trigger-object").getString("order_id"));
    }
}
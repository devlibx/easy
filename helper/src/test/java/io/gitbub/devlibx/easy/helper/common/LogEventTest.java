package io.gitbub.devlibx.easy.helper.common;


import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.Maps;
import junit.framework.TestCase;
import org.junit.Assert;

public class LogEventTest extends TestCase {

    public void testLogEventWithError() {
        LogEvent.setGlobalServiceName(null);

        boolean gotException = false;
        try {
            LogEvent.Builder.withEventType("test");
        } catch (Exception e) {
            gotException = true;
        }
        Assert.assertTrue(gotException);

        LogEvent.setGlobalServiceName("testing");

        gotException = false;
        try {
            LogEvent.Builder.withEventType("test");
        } catch (Exception e) {
            gotException = true;
        }
        Assert.assertFalse(gotException);
    }

    public void testLogEvent() {
        LogEvent.setGlobalServiceName("testing");
        LogEvent event = LogEvent.Builder
                .withEventTypeAndEntity("test", "user", "user_1")
                .data("key", "value")
                .build();
        assertEquals("user", event.getEntity().getType());
        assertEquals("user_1", event.getEntity().getId());
        assertEquals("test", event.getEventType());
        assertEquals("value", event.getData().getString("key", "no"));


        event = LogEvent.Builder
                .withEventTypeEventSubTypeAndEntity("test", "test_sub_type", "user", "user_1")
                .data("key", "value")
                .dimensions("key1", "value1", "key2", "value2")
                .dimensions("key3", "value3")
                .dimensions(Maps.of("key4", "value4"))
                .build();
        assertEquals("user", event.getEntity().getType());
        assertEquals("user_1", event.getEntity().getId());
        assertEquals("test", event.getEventType());
        assertEquals("test_sub_type", event.getEventSubType());
        assertEquals("value", event.getData().getString("key", "no"));

        assertEquals("value1", event.getDimensions().get("key1"));
        assertEquals("value2", event.getDimensions().get("key2"));
        assertEquals("value3", event.getDimensions().get("key3"));
        assertEquals("value4", event.getDimensions().get("key4"));

        System.out.println(JsonUtils.asJson(event));


        event = LogEvent.Builder
                .withEventTypeEventSubTypeAndEntity("test", "test_sub_type", "user", "user_1")
                .build();
        System.out.println(JsonUtils.asJson(event));
    }
}
package io.gitbub.devlibx.easy.helper.common;


import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import junit.framework.TestCase;
import org.junit.Assert;

public class LogEventTest extends TestCase {

    public void testLogEventWithError() {
        LogEvent.setGlobalServiceName(null);

        boolean gotException = false;
        try {
            LogEvent.Builder.withEventName("test");
        } catch (Exception e) {
            gotException = true;
        }
        Assert.assertTrue(gotException);

        LogEvent.setGlobalServiceName("testing");

        gotException = false;
        try {
            LogEvent.Builder.withEventName("test");
        } catch (Exception e) {
            gotException = true;
        }
        Assert.assertFalse(gotException);
    }

    public void testLogEvent() {
        LogEvent.setGlobalServiceName("testing");
        LogEvent event = LogEvent.Builder
                .withEventNameAndEntity("test", "user", "user_1")
                .data("key", "value")
                .build();
        assertEquals("user", event.getEntity().getType());
        assertEquals("user_1", event.getEntity().getId());
        assertEquals("test", event.getEventName());
        assertEquals("value", event.getData().getString("key", "no"));

        System.out.println(JsonUtils.asJson(event));
    }
}
package io.gitbub.harishb2k.easy.helper.map;

import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import junit.framework.TestCase;
import lombok.Data;

import java.util.UUID;

public class StringObjectMapTest extends TestCase {

    public void testStringObjectMap() {
        UUID u = UUID.randomUUID();
        StringObjectMap map = new StringObjectMap();
        map.put("int_key", 11);
        map.put("string_key", "str");
        map.put("bool_key", true);
        map.put("long_key", 100L);
        map.put("uuid_key", u);

        assertEquals(11, map.getInt("int_key").intValue());
        assertEquals("str", map.getString("string_key"));
        assertEquals(Boolean.TRUE, map.getBoolean("bool_key"));
        assertEquals(100L, map.getLong("long_key").longValue());
        assertEquals(u, map.getUUID("uuid_key"));
    }

    public void testWriteSting() {
        TestClass testClass = new TestClass();
        testClass.setStr("some string");
        testClass.setAnInt(11);
        StringHelper stringHelper = new StringHelper();
        assertNotNull(stringHelper.stringify(testClass));
        assertFalse(stringHelper.stringify(testClass).isEmpty());
    }

    @Data
    public static class TestClass {
        private String str;
        private int anInt;
    }
}
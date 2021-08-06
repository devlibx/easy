package io.gitbub.devlibx.easy.helper.map;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
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

    public void testStringObjectMapExt() {
        StringObjectMap map = StringObjectMap.of(
                "int", 10,
                "string", "str_1",
                "boolean", true,
                "str_boolean", "false",
                "str_int", "11");

        // Get typed values
        assertEquals(10, map.getInt("int").intValue());
        assertEquals("str_1", map.getString("string"));
        assertTrue(map.getBoolean("boolean"));

        // Auto conversion from string to boolean
        assertFalse(map.getBoolean("str_boolean"));

        // Auto conversion from string to int
        assertEquals(11, map.getInt("str_int").intValue());
    }


    public void testGetWithKeySeparator() {
        StringObjectMap map = StringObjectMap.of(
                "int", 10,
                "string", "str_1",
                "boolean", true,
                "boolean_false", false,
                "str_boolean", "false",
                "str_int", "11",
                "sub_1", StringObjectMap.of(
                        "a", "b",
                        "sub_2", StringObjectMap.of("int", 11),
                        "list", Arrays.asList("1", "2")
                )
        );

        assertEquals(10, map.path(".", "int", Integer.class).intValue());
        assertEquals("str_1", map.path(".", "string", String.class));
        assertTrue(map.path(".", "boolean", Boolean.class));
        assertEquals("b", map.path(".", "sub_1.a", String.class));
        assertEquals(11, map.path(".", "sub_1.sub_2.int", Integer.class).intValue());

        assertEquals(10, map.path("#", "int", Integer.class).intValue());
        assertEquals("str_1", map.path("#", "string", String.class));
        assertTrue(map.path("#", "boolean", Boolean.class));
        assertEquals("b", map.path("#", "sub_1#a", String.class));
        assertEquals(11, map.path("#", "sub_1#sub_2#int", Integer.class).intValue());

        assertEquals(10, map.path("int", Integer.class).intValue());
        assertEquals("str_1", map.path("string", String.class));
        assertTrue(map.path("boolean", Boolean.class));
        assertEquals("b", map.path("sub_1.a", String.class));
        assertEquals(11, map.path("sub_1.sub_2.int", Integer.class).intValue());

        assertTrue(map.isPathValueTrue("boolean"));
        assertTrue(map.isPathValueFalse("boolean_false"));
        assertTrue(map.isPathValueEqual("sub_1.sub_2.int", 11));
        assertFalse(map.isPathValueEqual("sub_1.sub_2.int", 12));
        assertTrue(map.isPathValueEqual("sub_1.a", "b"));
        assertFalse(map.isPathValueEqual("sub_1.a", "ba"));

        List l = map.path("sub_1.list", List.class);
        assertNotNull(l);
        assertEquals(2, l.size());
        assertEquals("1", l.get(0));
        assertEquals("2", l.get(1));
    }
}
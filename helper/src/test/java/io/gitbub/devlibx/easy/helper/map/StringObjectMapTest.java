package io.gitbub.devlibx.easy.helper.map;

import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
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

    public void testGetWithKeySeparator_2_100_times() {
        for (int i = 0 ;i < 20; i++) {
            testGetWithKeySeparator_2();
        }
    }

    public void testGetWithKeySeparator_2() {
        String s = "{\n" +
                "  \"_id\": \"61923ef95d7d2e1f2c20ebb3\",\n" +
                "  \"index\": 0,\n" +
                "  \"guid\": \"74c17e20-7ab5-4b97-8d3a-daf1563f3829\",\n" +
                "  \"isActive\": false,\n" +
                "  \"balance\": \"$3,827.04\",\n" +
                "  \"picture\": \"http://placehold.it/32x32\",\n" +
                "  \"age\": 25,\n" +
                "  \"eyeColor\": \"brown\",\n" +
                "  \"name\": \"Amalia Kane\",\n" +
                "  \"gender\": \"female\",\n" +
                "  \"company\": \"ATGEN\",\n" +
                "  \"email\": \"amaliakane@atgen.com\",\n" +
                "  \"phone\": \"+1 (819) 400-3482\",\n" +
                "  \"address\": \"856 Montauk Court, Waumandee, Rhode Island, 5510\",\n" +
                "  \"about\": \"Qui amet eiusmod ut proident laboris in pariatur officia duis culpa. Ex ipsum id ad aliquip. Elit adipisicing cupidatat magna velit eu qui laborum. Laborum eiusmod irure adipisicing et nostrud. Dolor ea cupidatat dolore aute nostrud nisi ex ipsum.\\r\\n\",\n" +
                "  \"registered\": \"2019-01-27T07:40:14 -06:-30\",\n" +
                "  \"latitude\": -41.835071,\n" +
                "  \"longitude\": 178.600322,\n" +
                "  \"tags\": [\n" +
                "    \"est\",\n" +
                "    \"occaecat\",\n" +
                "    \"eu\",\n" +
                "    \"mollit\",\n" +
                "    \"consectetur\",\n" +
                "    \"duis\",\n" +
                "    \"velit\"\n" +
                "  ],\n" +
                "  \"friends\": [\n" +
                "    {\n" +
                "      \"id\": 0,\n" +
                "      \"name\": \"Sherrie Burks\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"name\": \"Williams Kidd\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"name\": \"Morse Rodgers\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"greeting\": \"Hello, Amalia Kane! You have 4 unread messages.\",\n" +
                "  \"favoriteFruit\": \"apple\"\n" +
                "}";

        StringObjectMap  map = JsonUtils.convertAsStringObjectMap(s);
        long start = System.currentTimeMillis();
        for (int i = 0 ; i< 1_000_000; i++) {
            map.path("sub_1.sub_2.int", Object.class);
        }
        long end = System.currentTimeMillis();
        System.out.println("===> " + (end - start));
    }

    public void testSortedMap() {
        SortedMap<String, String> m = Maps.SortedMaps.of("a", "b", "c", "d");
        assertEquals("b", m.get("a"));
        assertEquals("d", m.get("c"));
    }
}
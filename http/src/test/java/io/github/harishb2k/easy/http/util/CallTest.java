package io.github.harishb2k.easy.http.util;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class CallTest extends TestCase {

    public void testServerAndApiProvided() {
        boolean gotError = false;
        try {
            Call call = Call.builder()
                    .asContentTypeJson()
                    .build();
        } catch (IllegalArgumentException e) {
            gotError = true;
        }
        assertTrue(gotError);
    }

    public void testHeaders() {

        // Test 1 - add a single key-value
        Call call = Call.builder()
                .withServerAndApi("server", "api")
                .addHeader("a", "b")
                .build();
        assertEquals("b", call.getHeaders().get("a"));

        // Test 2 - add 2 key-value
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addHeader("a", "b")
                .addHeader("c", "d")
                .build();
        assertEquals("b", call.getHeaders().get("a"));
        assertEquals("d", call.getHeaders().get("c"));

        // Test 3 - add 3 key-value (using var args)
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addHeaders("a", "b", "c", "d")
                .addHeader("e", "f")
                .build();
        assertEquals("b", call.getHeaders().get("a"));
        assertEquals("d", call.getHeaders().get("c"));
        assertEquals("f", call.getHeaders().get("e"));

        // Test 4 - add 3 key-value (using map)
        Map<String, Object> map = new HashMap<>();
        map.put("a1", "b1");
        map.put("c1", "d1");
        map.put("e1", "f1");
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addHeaders(map)
                .build();
        assertEquals("b1", call.getHeaders().get("a1"));
        assertEquals("d1", call.getHeaders().get("c1"));
        assertEquals("f1", call.getHeaders().get("e1"));

        // Test 4 - add 3 key-value (using map)
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addHeaders(map)
                .addHeaders("a", "b", "c", "d")
                .addHeader("e", "f")
                .build();
        assertEquals("b", call.getHeaders().get("a"));
        assertEquals("d", call.getHeaders().get("c"));
        assertEquals("f", call.getHeaders().get("e"));
        assertEquals("b1", call.getHeaders().get("a1"));
        assertEquals("d1", call.getHeaders().get("c1"));
        assertEquals("f1", call.getHeaders().get("e1"));

        // Test 5 - make sure odd no of var args fail
        boolean gotError = false;
        try {
            call = Call.builder()
                    .withServerAndApi("server", "api")
                    .addHeaders("a", "b", "c")
                    .addHeader("e", "f")
                    .build();
        } catch (RuntimeException e) {
            gotError = true;
        }
        assertTrue(gotError);
    }
}
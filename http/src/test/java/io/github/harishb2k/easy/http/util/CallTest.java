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

    public void testPathParams() {

        // Test 1 - add a single key-value
        Call call = Call.builder()
                .withServerAndApi("server", "api")
                .addPathParam("a", "b")
                .build();
        assertEquals("b", call.getPathParams().get("a"));

        // Test 2 - add 2 key-value
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addPathParam("a", "b")
                .addPathParam("c", "d")
                .build();
        assertEquals("b", call.getPathParams().get("a"));
        assertEquals("d", call.getPathParams().get("c"));

        // Test 3 - add 3 key-value (using var args)
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addPathParams("a", "b", "c", "d")
                .addPathParam("e", "f")
                .build();
        assertEquals("b", call.getPathParams().get("a"));
        assertEquals("d", call.getPathParams().get("c"));
        assertEquals("f", call.getPathParams().get("e"));

        // Test 4 - add 3 key-value (using map)
        Map<String, Object> map = new HashMap<>();
        map.put("a1", "b1");
        map.put("c1", "d1");
        map.put("e1", "f1");
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addPathParams(map)
                .build();
        assertEquals("b1", call.getPathParams().get("a1"));
        assertEquals("d1", call.getPathParams().get("c1"));
        assertEquals("f1", call.getPathParams().get("e1"));

        // Test 4 - add 3 key-value (using map)
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addPathParams(map)
                .addPathParams("a", "b", "c", "d")
                .addPathParam("e", "f")
                .build();
        assertEquals("b", call.getPathParams().get("a"));
        assertEquals("d", call.getPathParams().get("c"));
        assertEquals("f", call.getPathParams().get("e"));
        assertEquals("b1", call.getPathParams().get("a1"));
        assertEquals("d1", call.getPathParams().get("c1"));
        assertEquals("f1", call.getPathParams().get("e1"));

        // Test 5 - make sure odd no of var args fail
        boolean gotError = false;
        try {
            call = Call.builder()
                    .withServerAndApi("server", "api")
                    .addPathParams("a", "b", "c")
                    .addPathParam("e", "f")
                    .build();
        } catch (RuntimeException e) {
            gotError = true;
        }
        assertTrue(gotError);
    }

    public void testQueryParams() {

        // Test 1 - add a single key-value
        Call call = Call.builder()
                .withServerAndApi("server", "api")
                .addQueryParam("a", "b")
                .build();
        assertEquals("b", call.getQueryParam().get("a").get(0));

        // Test 2 - add 2 key-value
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addQueryParam("a", "b")
                .addQueryParam("c", "d")
                .build();
        assertEquals("b", call.getQueryParam().get("a").get(0));
        assertEquals("d", call.getQueryParam().get("c").get(0));

        // Test 3 - add 3 key-value (using var args)
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addQueryParams("a", "b", "c", "d")
                .addQueryParam("e", "f")
                .build();
        assertEquals("b", call.getQueryParam().get("a").get(0));
        assertEquals("d", call.getQueryParam().get("c").get(0));
        assertEquals("f", call.getQueryParam().get("e").get(0));

        // Test 4 - add 3 key-value (using map)
        Map<String, Object> map = new HashMap<>();
        map.put("a1", "b1");
        map.put("c1", "d1");
        map.put("e1", "f1");
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addQueryParams(map)
                .build();
        assertEquals("b1", call.getQueryParam().get("a1").get(0));
        assertEquals("d1", call.getQueryParam().get("c1").get(0));
        assertEquals("f1", call.getQueryParam().get("e1").get(0));

        // Test 4 - add 3 key-value (using map)
        call = Call.builder()
                .withServerAndApi("server", "api")
                .addQueryParams(map)
                .addQueryParams("a", "b", "c", "d")
                .addQueryParam("e", "f")
                .build();
        assertEquals("b", call.getQueryParam().get("a").get(0));
        assertEquals("d", call.getQueryParam().get("c").get(0));
        assertEquals("f", call.getQueryParam().get("e").get(0));
        assertEquals("b1", call.getQueryParam().get("a1").get(0));
        assertEquals("d1", call.getQueryParam().get("c1").get(0));
        assertEquals("f1", call.getQueryParam().get("e1").get(0));

        // Test 5 - make sure odd no of var args fail
        boolean gotError = false;
        try {
            call = Call.builder()
                    .withServerAndApi("server", "api")
                    .addQueryParams("a", "b", "c")
                    .addQueryParam("e", "f")
                    .build();
        } catch (RuntimeException e) {
            gotError = true;
        }
        assertTrue(gotError);
    }
}
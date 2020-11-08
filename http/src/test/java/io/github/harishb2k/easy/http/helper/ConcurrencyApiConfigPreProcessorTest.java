package io.github.harishb2k.easy.http.helper;

import io.github.harishb2k.easy.http.config.Api;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcurrencyApiConfigPreProcessorTest {

    @Test
    public void testWhenRpsNotGiven() {
        Api api = new Api();
        api.setConcurrency(10);
        api.setTimeout(100);
        api.setRps(0);

        ConcurrencyApiConfigPreProcessor preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(100, api.getTimeout());
        assertEquals(10, api.getConcurrency());
    }

    @Test
    public void testWhenRpsIsGiven() {
        // Test 1
        Api api = new Api();
        api.setConcurrency(10);
        api.setTimeout(100);
        api.setRps(90);
        ConcurrencyApiConfigPreProcessor preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(100, api.getTimeout());
        assertEquals(9, api.getConcurrency());

        // Test 2
        api = new Api();
        api.setConcurrency(10);
        api.setTimeout(1000);
        api.setRps(90);
        preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(1000, api.getTimeout());
        assertEquals(90, api.getConcurrency());

        // Test 3
        api = new Api();
        api.setConcurrency(10);
        api.setTimeout(2000);
        api.setRps(90);
        preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(2000, api.getTimeout());
        assertEquals(180, api.getConcurrency());

        // Test 4
        api = new Api();
        api.setConcurrency(10);
        api.setTimeout(50);
        api.setRps(1000);
        preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(50, api.getTimeout());
        assertEquals(50, api.getConcurrency());


        // Test 5
        api = new Api();
        api.setConcurrency(10);
        api.setTimeout(1);
        api.setRps(1000);
        preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(1, api.getTimeout());
        assertEquals(1, api.getConcurrency());

        // Test 5
        api = new Api();
        api.setConcurrency(10);
        api.setTimeout(10);
        api.setRps(100);
        preProcessor = new ConcurrencyApiConfigPreProcessor();
        preProcessor.process("dummy", api);
        assertEquals(10, api.getTimeout());
        assertEquals(1, api.getConcurrency());
    }
}
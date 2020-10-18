package io.gitbub.harishb2k.easy.helper;

import junit.framework.TestCase;
import lombok.Data;

public class JsonUtilTest extends TestCase {
    public void testWriteString() {
        TestClass testClass = new TestClass();
        testClass.setStr("some string");
        testClass.setAnInt(11);

        JsonUtil jsonUtil = new JsonUtil();
        String result = jsonUtil.writeString(testClass);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testClass, jsonUtil.readObject(result, TestClass.class));
    }

    @Data
    private static class TestClass {
        private String str;
        private int anInt;
    }
}
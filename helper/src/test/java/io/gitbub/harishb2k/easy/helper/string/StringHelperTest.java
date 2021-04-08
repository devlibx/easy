package io.gitbub.devlibx.easy.helper.string;

import junit.framework.TestCase;
import lombok.Data;

public class StringHelperTest extends TestCase {
    public void testWriteSting() {
        PojoClass testClass = new PojoClass();
        testClass.setStr("some string");
        testClass.setAnInt(11);
        StringHelper stringHelper = new StringHelper();
        assertNotNull(stringHelper.stringify(testClass));
        assertFalse(stringHelper.stringify(testClass).isEmpty());
        System.out.println(stringHelper.stringify(testClass));
    }

    @Data
    public static class PojoClass {
        private String str;
        private int anInt;
    }
}
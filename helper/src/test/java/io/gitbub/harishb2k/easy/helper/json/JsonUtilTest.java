package io.gitbub.harishb2k.easy.helper.json;

import io.gitbub.harishb2k.easy.helper.map.StringObjectMapTest.TestClass;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import junit.framework.TestCase;

public class JsonUtilTest extends TestCase {

    public void testWriteString() {
        TestClass testClass = new TestClass();
        testClass.setStr("some string");
        testClass.setAnInt(11);
        StringHelper stringHelper = new StringHelper();
        JsonUtil jsonUtil = new JsonUtil();
        assertEquals(testClass, jsonUtil.readObject(stringHelper.stringify(testClass), TestClass.class));
    }
}
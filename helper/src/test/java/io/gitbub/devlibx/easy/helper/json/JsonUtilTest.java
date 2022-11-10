package io.gitbub.devlibx.easy.helper.json;

import io.gitbub.devlibx.easy.helper.string.StringHelper;
import io.gitbub.devlibx.easy.helper.string.StringHelperTest.PojoClass;
import junit.framework.TestCase;
import org.joda.time.DateTime;

public class JsonUtilTest extends TestCase {

    public void testWriteString() {
        PojoClass testClass = new PojoClass();
        testClass.setStr("some string");
        testClass.setAnInt(11);
        testClass.setDateTime(DateTime.now());
        StringHelper stringHelper = new StringHelper();
        JsonUtil jsonUtil = new JsonUtil();
        assertEquals(testClass.getAnInt(), jsonUtil.readObject(stringHelper.stringify(testClass), PojoClass.class).getAnInt());
        assertEquals(testClass.getStr(), jsonUtil.readObject(stringHelper.stringify(testClass), PojoClass.class).getStr());
        assertEquals(testClass.getDateTime().getMillis(), jsonUtil.readObject(stringHelper.stringify(testClass), PojoClass.class).getDateTime().getMillis());
    }
}
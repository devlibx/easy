package io.gitbub.harishb2k.easy.helper.string;

import io.gitbub.harishb2k.easy.helper.json.JsonUtil;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Common string function to be used.
 */
public class StringHelper {
    private static final JsonUtil jsonUtil = new JsonUtil();

    /**
     * Helper to convert object to string
     */
    public static String stringify(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof String) {
            return obj.toString();
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj instanceof DateTime) {
            return ((DateTime) obj).getMillis() + "";
        } else if (obj instanceof UUID) {
            return obj.toString();
        } else {
            return jsonUtil.writeString(obj);
        }
    }
}

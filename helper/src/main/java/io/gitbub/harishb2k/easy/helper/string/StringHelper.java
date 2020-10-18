package io.gitbub.harishb2k.easy.helper.string;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gitbub.harishb2k.easy.helper.json.JsonUtil;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Common string function to be used.
 */
public class StringHelper {
    private final JsonUtil jsonUtil;

    public StringHelper() {
        this.jsonUtil = new JsonUtil();
    }

    /**
     * Build StringHelper with custom JsonUtil
     */
    @Inject
    public StringHelper(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    /**
     * Helper to convert object to string
     */
    public String stringify(Object obj) {
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
            try {
                return jsonUtil.getObjectMapper().writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

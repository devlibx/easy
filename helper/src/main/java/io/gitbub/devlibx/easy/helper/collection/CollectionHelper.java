package io.gitbub.devlibx.easy.helper.collection;

import java.util.List;

public class CollectionHelper {

    @SuppressWarnings("rawtypes")
    public static Object[] convertListToObjectArray(Object object) {
        if (object instanceof List) {
            return convertListToObjectArray((List) object);
        } else {
            return new Object[0];
        }
    }

    @SuppressWarnings("rawtypes")
    public static Object[] convertListToObjectArray(List list) {

        // Empty array if blank list is passed
        if (list == null) return new Object[0];

        // Build object array
        Object[] output = new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            output[i] = list.get(i);
        }

        return output;
    }
}

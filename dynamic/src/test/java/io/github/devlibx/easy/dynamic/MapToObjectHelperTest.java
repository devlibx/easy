package io.github.devlibx.easy.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class MapToObjectHelperTest {

    @Test
    public void testMap() throws Exception {
        MapToObjectHelper helper = new MapToObjectHelper();

        Map<String, Object> input = new HashMap<>();
        input.put("intValue", 10);
        input.put("floatValue", 10.0f);
        input.put("doubleValue", 10.0);
        input.put("booleanValue", 10.0);
        input.put("stringValue", "my_str");
        Map<String, Object> subMap = new HashMap<>();
        subMap.put("intValueInSubMap", 12);
        input.put("mapValue", subMap);

        input.put("nullValue", null);

        input.put("emptyList", Arrays.asList());
        input.put("list", Arrays.asList(10, 11));

        Object output = helper.convertMapToDynamicClass(Object.class, input);

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(output);
        Assertions.assertNotNull(json);

        System.out.println(json);

    }
}
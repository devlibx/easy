### StringObjectMap

A useful lib to work with Map with string key andy any objecy
<hr>

#### Path finding
This map allows a way to find the data in sub-path. It is like finding path using json path "$.body.id". However, we
only support non-array path e.g. "body.id". <br><br>
This is a custom implementation and does not uses JsonPath lib

```java
public class Example {
    public void pathExample() {
        StringObjectMap map = JsonUtils.convertAsStringObjectMap("some json string");

        // Read a value in path "body.entity.id"
        Integer intValue = map.path(".", "body.user.id", Integer.class);
        System.out.println(intValue);

        // Read a value in path "body.entity.name"
        String stringValue = map.path(".", "body.user.name", String.class);
        System.out.println(stringValue);

        // Check value at path
        boolean result = false;
        result = map.isPathValueTrue("body.user.enabled");
        result = map.isPathValueFalse("body.user.deleted");

        // Check value at path (you can pass int, boolean, or any other object)
        result = map.isPathValueEqual("body.user.id", 11);
    }
}
```
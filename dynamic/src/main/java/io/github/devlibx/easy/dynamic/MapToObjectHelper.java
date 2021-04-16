package io.github.devlibx.easy.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import net.sf.cglib.core.ClassGenerator;
import net.sf.cglib.core.DefaultGeneratorStrategy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import net.sf.cglib.transform.TransformingClassGenerator;
import net.sf.cglib.transform.impl.AddPropertyTransformer;
import org.apache.commons.beanutils.PropertyUtils;
import org.objectweb.asm.Type;

import java.util.*;

import static io.gitbub.devlibx.easy.helper.collection.CollectionHelper.convertListToObjectArray;

@SuppressWarnings("unchecked")
public class MapToObjectHelper {
    private Map<Set<String>, Enhancer> classCache = new HashMap<>();

    public static void main(String[] args) throws Exception {

        MapToObjectHelper e1 = new MapToObjectHelper();
        Map<String, Object> map = new HashMap<>();
        map.put("t", 21);

        Map<String, Object> input = new StringObjectMap();
        input.put("rain", true);

        for (int j = 0; j < 100; j++) {
            input.put("rain_" + j, true);
        }

        input.put("fail", 10);
        input.put("e", e1);
        input.put("map", map);
        input.put("list", Arrays.asList(17, 11));

        MapToObjectHelper helper = new MapToObjectHelper();
        Object output = helper.generateDynamicClassFromMap(input);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            output = helper.generateDynamicClassFromMap(input);
            // System.out.println(output);
        }
        System.out.println("Time=" + (System.currentTimeMillis() - start));

        ObjectMapper om = new ObjectMapper();
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // System.out.println(output.getClass().getName());
        // System.out.println(om.writeValueAsString(output));

    }


    public Object generateDynamicClassFromMap(Map<String, Object> object) throws Exception {

        // Set top level properties first from input
        Set<String> properties = new TreeSet<>();
        populateProperties(object, properties);
        setupClasses(object, properties);

        // We must have the parent class by now
        Enhancer e = classCache.get(properties);
        Object parentObject = e.create();

        // Arrays.stream(parentObject.getClass().getDeclaredFields()).forEach(field -> System.out.println(field.getName()));

        // Build child classes if required
        for (String key : object.keySet()) {

            // Do not include null or empty
            if (Strings.isNullOrEmpty(key) || object.get(key) == null) continue;

            // Handle Maps
            if (object.get(key) instanceof Map) {
                Object child = generateDynamicClassFromMap((Map<String, Object>) object.get(key));
                PropertyUtils.setSimpleProperty(parentObject, key, child);
            } else if (object.get(key) instanceof List) {
                PropertyUtils.setSimpleProperty(parentObject, key, convertListToObjectArray(object.get(key)));
            } else {
                PropertyUtils.setSimpleProperty(parentObject, key, object.get(key));
            }
        }

        return parentObject;
    }

    private void setupClasses(Map<String, Object> object, Set<String> properties) {

        // Fill all properties first if it is empty
        if (properties.isEmpty()) {
            populateProperties(object, properties);
        }

        // Add a new enhancer if we don't have it already
        if (!classCache.containsKey(properties)) {
            Enhancer e = new Enhancer();
            e.setSerialVersionUID(System.currentTimeMillis() + (new Random().nextInt()));
            e.setCallback(NoOp.INSTANCE);
            e.setStrategy(new DefaultGeneratorStrategy() {
                @Override
                protected ClassGenerator transform(ClassGenerator cg) {
                    return new TransformingClassGenerator(cg, generateAddPropertyTransformer(object));
                }
            });
            classCache.put(properties, e);
        }

        // Check each property and create new enhancer if property type is map
        for (String prop : properties) {
            if (object.get(prop) instanceof Map) {
                setupClasses((Map<String, Object>) object.get(prop), new TreeSet<>());
            }
        }
    }


    private void populateProperties(Map<String, Object> object, Set<String> properties) {
        properties.addAll(object.keySet());
    }

    public <T> T convertMapToDynamicClass(Class<T> cls, Map<String, Object> object) throws Exception {
        Enhancer e = new Enhancer();
        e.setSuperclass(cls);
        e.setCallback(NoOp.INSTANCE);
        e.setStrategy(new DefaultGeneratorStrategy() {
            @Override
            protected ClassGenerator transform(ClassGenerator cg) {
                return new TransformingClassGenerator(cg, generateAddPropertyTransformer(object));
            }
        });

        Object obj = e.create();
        for (String key : object.keySet()) {
            // Do not include null or empty
            if (Strings.isNullOrEmpty(key) || object.get(key) == null) continue;

            if (object.get(key) instanceof List) {
                PropertyUtils.setSimpleProperty(obj, key, convertListToObjectArray(object.get(key)));
            } else {
                PropertyUtils.setSimpleProperty(obj, key, object.get(key));
            }
        }
        return (T) obj;
    }

    private AddPropertyTransformer generateAddPropertyTransformer(Map<String, Object> object) {
        List<String> properties = new ArrayList<>();
        List<Type> types = new ArrayList<>();
        for (String key : object.keySet()) {

            // Do not include null or empty
            if (Strings.isNullOrEmpty(key) || object.get(key) == null) continue;

            properties.add(key);

            if (object.get(key) instanceof List) {
                types.add(Type.getType(Object[].class));
            } else if (object.get(key) instanceof Map) {
                types.add(Type.getType(Object.class));
            } else {
                types.add(Type.getType(object.get(key).getClass()));
            }
        }

        String[] keys = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            keys[i] = properties.get(i);
        }

        Type[] _types = new Type[types.size()];
        for (int i = 0; i < types.size(); i++) {
            _types[i] = types.get(i);
        }
        return new AddPropertyTransformer(keys, _types);
    }
}

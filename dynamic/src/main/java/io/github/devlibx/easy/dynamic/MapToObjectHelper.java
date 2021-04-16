package io.github.devlibx.easy.dynamic;

import com.google.common.base.Strings;
import net.sf.cglib.core.ClassGenerator;
import net.sf.cglib.core.DefaultGeneratorStrategy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import net.sf.cglib.transform.TransformingClassGenerator;
import net.sf.cglib.transform.impl.AddPropertyTransformer;
import org.apache.commons.beanutils.PropertyUtils;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.gitbub.devlibx.easy.helper.collection.CollectionHelper.convertListToObjectArray;

@SuppressWarnings("unchecked")
public class MapToObjectHelper {

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

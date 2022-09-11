package io.gitbub.devlibx.easy.helper.yaml;

import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static io.gitbub.devlibx.easy.helper.file.FileHelper.readFileFromResourcePath;
import static io.gitbub.devlibx.easy.helper.file.FileHelper.readStream;

public class YamlUtils {
    /**
     * Read YAM file (Camel Case) and convert to a object
     */
    public static <T> T readYamlCamelCase(String file, Class<T> cls) {
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load(readStream(file));
        return JsonUtils.getCamelCase().readObject(new StringHelper().stringify(obj), cls);
    }

    /**
     * Read YAM file and convert to an object
     */
    public static <T> T readYaml(String file, Class<T> cls) {
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load(readStream(file));
        return JsonUtils.getCamelCase().readObject(new StringHelper().stringify(obj), cls);
    }

    /**
     * Read file from resources path
     *
     * @param file file coming from "resources" dir
     */
    public static <T> T readYamlFromResourcePath(String file, Class<T> cls) {
        try {
            String configAsString = readFileFromResourcePath(file);
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(configAsString);
            return JsonUtils.getCamelCase().readObject(new StringHelper().stringify(obj), cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

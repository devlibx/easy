package io.gitbub.devlibx.easy.helper.file;

import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@SuppressWarnings("ConstantConditions")
@Slf4j
public class FileHelper {

    public static InputStream readStream(String file) {
        log.info("Reading file=[{}] from path={}", file, Thread.currentThread().getContextClassLoader().getResource(file));
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
    }

    public static String read(String file) {
        try {
            log.info("Reading file=[{}] from path={}", file, Thread.currentThread().getContextClassLoader().getResource(file));
            return IOUtils.toString(readStream(file), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read file content from resource path
     */
    public static String readFileFromResourcePath(String file) {
        try {
            InputStream in = YamlUtils.class.getResourceAsStream(file);
            if (in != null) {
                return IOUtils.toString(in, Charset.defaultCharset());
            } else {
                throw new RuntimeException("did not find file=" + file + " in resources");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package io.gitbub.harishb2k.easy.helper.file;

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
}

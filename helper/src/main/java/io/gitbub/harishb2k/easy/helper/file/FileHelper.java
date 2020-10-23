package io.gitbub.harishb2k.easy.helper.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

@SuppressWarnings("ConstantConditions")
@Slf4j
public class FileHelper {
    public static String read(String file) {
        try {
            log.info("Reading file=[{}] from path={}", file, Thread.currentThread().getContextClassLoader().getResource(file));
            return IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream(file), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

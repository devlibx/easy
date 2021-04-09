package io.gitbub.devlibx.easy.helper;

import org.junit.jupiter.api.BeforeEach;

public abstract class CommonBaseTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        try {
            LoggingHelper.setupLogging();
        } catch (Exception ignored) {
        }
    }
}

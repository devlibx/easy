package io.gitbub.harishb2k.easy.helper;

import junit.framework.TestCase;

public abstract class CommonBaseTestCase extends TestCase {

    @Override
    public void setUp() throws Exception {
        try {
            LoggingHelper.setupLogging();
        } catch (Exception ignored) {
        }
    }
}

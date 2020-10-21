package io.gitbub.harishb2k.easy.helper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Safe {
    private static final Logger logger = Logger.getLogger("safe");

    public static void safe(Runnable r, String errorString) {
        try {
            r.run();
        } catch (Throwable e) {
            logger.log(Level.WARNING, errorString);
            e.printStackTrace();
        }
    }

    public static void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

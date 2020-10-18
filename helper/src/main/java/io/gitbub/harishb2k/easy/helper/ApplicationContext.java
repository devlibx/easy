package io.gitbub.harishb2k.easy.helper;

import com.google.inject.Injector;
import com.google.inject.Key;

import java.util.Optional;

public class ApplicationContext {
    private static Injector injector;

    /**
     * @param injector singleton injector to be used for objects
     */
    public static void setInjector(Injector injector) {
        ApplicationContext.injector = injector;
    }

    /**
     * @param cls type of class to request
     * @param <T> type of class to request
     * @return instance of request type
     */
    public static <T> T getInstance(Class<T> cls) {
        return injector.getInstance(cls);
    }

    /**
     * @param key type of class to request with help of key
     * @param <T> type of class to request with help of key
     * @return instance of request type
     */
    public static <T> T getInstance(Key<T> key) {
        return injector.getInstance(key);
    }

    /**
     * @param cls type of class to request
     * @param <T> type of class to request
     * @return instance of request type or Optional.empty() otherwise
     */
    public static <T> Optional<T> getOptionalInstance(Class<T> cls) {
        try {
            T t = getInstance(cls);
            return t == null ? Optional.empty() : Optional.of(t);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

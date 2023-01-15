package io.gitbub.devlibx.easy.helper;

public class DoNotUse {

    public static void main(String[] args) {
        System.out.println(formatter(1));
        System.out.println(formatter("1"));
        System.out.println(formatter(true));
    }

    public static String formatter(Object o) {
        return "na";
    }

    public static void testVirtualThread() {
       throw new RuntimeException("Not implemented");
    }

    public static boolean isJava19Enabled() {
        return false;
    }
}

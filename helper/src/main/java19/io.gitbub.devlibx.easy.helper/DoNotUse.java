package io.gitbub.devlibx.easy.helper;

public class DoNotUse {

    public static void main(String[] args) {
        System.out.println(formatter(1));
        System.out.println(formatter("1"));
        System.out.println(formatter(true));
    }

    public static String formatter(Object o) {
        String formatted = "unknown";
        if (o instanceof Integer i) {
            formatted = String.format("int %d", i);
        } else if (o instanceof Long l) {
            formatted = String.format("long %d", l);
        } else if (o instanceof Double d) {
            formatted = String.format("double %f", d);
        } else if (o instanceof String s) {
            formatted = String.format("String %s", s);
        }
        return formatted;
    }

    public static void testVirtualThread() {
        Thread.ofVirtual().start(new Runnable() {
            @Override
            public void run() {
                System.out.println("java 19 working from easy 19....");
            }
        });
    }

    public static boolean isJava19Enabled() {
        return true;
    }
}

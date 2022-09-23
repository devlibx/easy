package io.gitbub.devlibx.easy.helper;

class DoNotUse {

    public static void main(String[] args) {
        System.out.println(formatter(1));
        System.out.println(formatter("1"));
        System.out.println(formatter(true));
    }

    static String formatter(Object o) {
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
}

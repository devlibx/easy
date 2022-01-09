package io.gitbub.devlibx.easy.helper.map;

import java.util.ArrayList;
import java.util.List;

class CodeGenerator {

    public static void main(String[] args) {
        // Generate(10, "Map<String, String>", "HashMap<>()", "String", "String");
        Generate(10, "Map<K, V>", "HashMap<>()", "K", "V", "<K, V>");
    }

    public static void Generate(int count, String className, String classNameImpl, String keyType, String valueType, String genericReturnType) {
        for (int i = 1; i <= count; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("public static ").append(genericReturnType).append(" ").append(className).append(" of(");

            List<String> argParam = new ArrayList<>();
            for (int j = 1; j <= i; j++) {
                argParam.add(String.format("%s key%d, %s value%d", keyType, j, valueType, j));
            }
            String string = String.join(", ", argParam);
            sb.append(string);

            sb.append(") {\n\t");
            sb.append(className).append(" map = new ").append(classNameImpl).append(";\n");

            argParam = new ArrayList<>();
            for (int j = 1; j <= i; j++) {
                argParam.add(String.format("\tmap.put(key%d, value%d)", j, j));
            }
            string = String.join(";\n", argParam);
            sb.append(string);

            sb.append("; \n \treturn map;");
            sb.append("\n} \n");

            System.out.println(sb);
        }

    }
}

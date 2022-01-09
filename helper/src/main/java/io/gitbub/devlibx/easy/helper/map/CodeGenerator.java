package io.gitbub.devlibx.easy.helper.map;

import java.util.ArrayList;
import java.util.List;

class CodeGenerator {
    public static void main(String[] args) {

        int N = 10;
        for (int i = 1; i <= N; i++) {

            String className = "Map<String, String>";
            String classNameImpl = "HashMap<>()";

            StringBuffer sb = new StringBuffer();
            sb.append("public static ").append(className).append(" of(");


            List<String> argParam = new ArrayList<>();
            for (int j = 1; j <= i; j++) {
                argParam.add(String.format("String key%d, String value%d", j, j));
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

            System.out.println(sb.toString());
        }

    }
}

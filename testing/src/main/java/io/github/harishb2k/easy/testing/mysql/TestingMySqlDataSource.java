package io.github.harishb2k.easy.testing.mysql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestingMySqlDataSource {
    String value() default MySqlExtension.DEFAULT_DATASOURCE_NAME;
}

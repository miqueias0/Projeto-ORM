package br.com.mike.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JoinColumn {
    String[] value() default {};

}

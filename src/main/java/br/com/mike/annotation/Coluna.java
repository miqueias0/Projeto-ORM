package br.com.mike.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Coluna {

    String nome();
    Class<?> typeBD() default Object.class;
    boolean unica() default false;

    boolean alteravel() default true;

    boolean inserivel() default true;

    boolean anulavel() default true;

    boolean chavePrimaria() default false;

    boolean chaveEstrangeira() default false;

    int tamanho() default 255;
}

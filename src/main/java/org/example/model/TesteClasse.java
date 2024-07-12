package org.example.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;

public class TesteClasse<T> {

    public T createObject(Class<?> clazz) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        T value = (T) clazz.getConstructor().newInstance();
        for (Field field : fields) {
            field.setAccessible(true);
            if (String.class.equals(field.getType())) {
                field.set(value, "teste");
            } else if (BigDecimal.class.equals(field.getType())) {
                field.set(value, BigDecimal.ZERO);
            } else if (Date.class.equals(field.getType())) {
                field.set(value, new Date());
            }
            System.out.println("Field: " + field.getName());
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            System.out.println("Method: " + method.getName());
        }
        return value;
    }
}

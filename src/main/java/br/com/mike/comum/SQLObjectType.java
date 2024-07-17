package br.com.mike.comum;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class SQLObjectType {
    public static int type(Object valor) {
        if (valor == null) {
            return Types.NULL;
        }
        Map<Class, Integer> map = new HashMap<>(Map.of(String.class, 0, Number.class, 1,
                BigDecimal.class, 1, Boolean.class, 2, Date.class, 3, Timestamp.class, 4,
                byte[].class, 5, Byte.class, 5, Object[].class, 6, java.util.ArrayList.class, 6));
        map.put(java.sql.Date.class, 3);
        map.put(Array.class, 6);
        map.put(java.sql.Array.class, 6);
        if (map.containsKey(valor.getClass())) {
            switch (map.get(valor.getClass())) {
                case 0:
                    return Types.VARCHAR;
                case 1:
                    return Types.NUMERIC;
                case 2:
                    return Types.BOOLEAN;
                case 3:
                    return Types.DATE;
                case 4:
                    return Types.TIMESTAMP;
                case 5:
                    return Types.BINARY;
                case 6:
                    return Types.ARRAY;

            }
        }
        return Types.OTHER;
    }

    public static String typeString(Object valor) {
        if (valor == null) {
            return "null";
        }
        Map<Class, Integer> map = new HashMap<>(Map.of(String.class, 0, Number.class, 1,
                BigDecimal.class, 1, Boolean.class, 2, Date.class, 3, Timestamp.class, 4,
                byte[].class, 5, Byte.class, 5, Object[].class, 6, java.util.ArrayList.class, 6));
        map.put(Array.class, 6);
        map.put(java.sql.Array.class, 6);
        if (map.containsKey(valor.getClass())) {
            switch (map.get(valor.getClass())) {
                case 0:
                    return "varchar";
                case 1:
                    return "numeric";
                case 2:
                    return "boolean";
                case 3:
                    return "date";
                case 4:
                    return "timestamp";
                case 5:
                    return "binary";
                case 6:
                    return "array";

            }
        }
        return "other";
    }
}

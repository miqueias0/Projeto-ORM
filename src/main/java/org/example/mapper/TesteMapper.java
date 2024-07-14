package org.example.mapper;

import br.com.ns2e.comum.bd.Comando;
import org.example.annotation.NomeAlternativo;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.*;

public class TesteMapper {

    public static Map<String, Long> map = new HashMap<>();

    public static <T> T mapearObjeto(Class<T> classe, ResultSet resultSet) throws Exception {
        return percorrerClasse(classe, resultSet);
    }

    private static <T> T percorrerClasse(Class<T> classe, ResultSet resultSet) throws Exception {
        T value = classe.getConstructor().newInstance();
        Field[] fields = value.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.getType().getPackageName().contains("java.")) {
                map.put(field.getType().getName() + ";" + classe.getTypeName(),
                        map.containsKey(field.getType().getName() + ";" + classe.getTypeName()) ?
                                map.get(field.getType().getName() + ";" + classe.getTypeName()) + 1 : 1);
            }
            field.setAccessible(true);
            if (!field.getType().getPackageName().contains("java.")
                    && map.containsKey(field.getType().getName() + ";" + classe.getTypeName())
                    && map.get(field.getType().getName() + ";" + classe.getTypeName()).compareTo(1L) > 0) {
                field.set(value, null);
            } else if (!field.toString().contains(" static ")) {
                field.set(value, percorrerClasse(field, resultSet));
            }
            field.setAccessible(false);
        }
        return value;
    }

    private static Object percorrerClasse(Field field, ResultSet result) throws Exception {
        if (field.getType().getPackageName().contains("java.")) {
            return field.getType().isPrimitive() ? cast(setValue(field.getType(), result, montarListaNames(field)), field.getType()) : setValue(field.getType(), result, montarListaNames(field));
        } else {
            return TesteMapper.mapearObjeto(field.getType(), result);
        }
    }

    private static List<String> montarListaNames(Field field) {
        List<String> variablesNames = new ArrayList<>();
        variablesNames.add(field.getName());
        variablesNames.add(join(field.getName()));
        NomeAlternativo nomeAlternativo = field.getAnnotation(NomeAlternativo.class);
        if (nomeAlternativo != null) {
            variablesNames.addAll(Arrays.stream(nomeAlternativo.value()).toList());
        }
        return variablesNames;
    }

    private static Object setValue(Class<?> classe, ResultSet resultSet, List<String> variablesName) throws Exception {
        for (String variableName : variablesName) {
            if (!Comando.existeColuna(resultSet, variableName)) {
                continue;
            }
            return cast(resultSet.getObject(variableName), classe);
        }
        return null;
    }

    private final static Map<String, Integer> mapPrimitiveType = new HashMap<>(
            Map.of(
                    int.class.getTypeName(), 0,
                    boolean.class.getTypeName(), 1,
                    char.class.getTypeName(), 2,
                    double.class.getTypeName(), 3,
                    float.class.getTypeName(), 4,
                    long.class.getTypeName(), 5,
                    byte.class.getTypeName(), 6
//                    ,short.class.getTypeName(), 7
                    ));

    private static <T> T cast(Object obj, Class<?> classe) {
        if (obj == null && classe.isPrimitive()) {
            switch (mapPrimitiveType.get(classe.getTypeName().toLowerCase())) {
                case 0:
                    return (T) (Object) Integer.parseInt("0");
                case 1:
                    return (T) Boolean.FALSE;
                case 2:
                    return (T) (Object) Character.MIN_VALUE;
                case 3:
                    return (T) Double.valueOf("0.00");
                case 4:
                    return (T) Float.valueOf("0.00");
                case 5:
                    return (T) (Object) Long.parseLong("0");
                case 6:
                    return (T) (Object) Byte.BYTES;
//                case 7:
//                    return (T) (Object) Short.;
            }
        }else if(obj != null && classe.getSimpleName().equalsIgnoreCase("String")){
            return (T) obj.toString();
        }
        return (T) obj;
    }

    private static Boolean isInstance(Object object, Class<?> classe) {
        return classe.isInstance(object);
    }

    private static String join(String value) {
        return join(value.toCharArray(), 0, "");
    }

    private static String join(char[] caracters, Integer position, String value) {
        value += (String.valueOf(caracters[position]).matches("^[A-Z]+$") ? "_" : "") + String.valueOf(caracters[position]).toLowerCase();
        if ((caracters.length - 1) > position) {
            return join(caracters, ++position, value);
        }
        return value;
    }
}

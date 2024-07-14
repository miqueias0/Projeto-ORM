package br.com.mike.data.operations;

import br.com.mike.annotation.Coluna;
import br.com.mike.annotation.Tabela;
import br.com.mike.comum.StringOperations;

import java.lang.reflect.Field;

public class CRUDManager<T> {

    private StringBuilder createSQLComandInsert(T value) throws Exception {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("INSERT INTO ").append(tableName(value.getClass()));
        textSQL.append(columnsName(value.getClass()));

        return textSQL;
    }

    public int save(T value) {


        return 0;
    }

    private String tableName(Class<?> clazz) throws Exception {
        Tabela tabela = clazz.getAnnotation(Tabela.class);
        if (tabela == null || tabela.value() == null || tabela.value().isBlank()) {
            throw new Exception("Table name can't defined");
        }
        return tabela.value();
    }

    private StringBuilder columnsName(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder columns = new StringBuilder();
        String delimitador = "";
        for (Field field : fields) {
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (coluna != null) {
                columns.append(delimitador).append(coluna.nome());
                continue;
            }
            columns.append(delimitador).append(StringOperations.join(field.getName()));
        }
        return columns;
    }


}

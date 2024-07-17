package br.com.mike.data.operations;

import br.com.mike.annotation.Coluna;
import br.com.mike.annotation.ManyToOne;
import br.com.mike.annotation.Tabela;
import br.com.mike.annotation.JoinColumn;
import br.com.mike.comum.SQLObjectType;
import br.com.mike.comum.StringOperations;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CRUDManager<T> {

    private final Connection connection;
    private PreparedStatement statement;

    public CRUDManager(Connection connection) {
        this.connection = connection;
    }

    private StringBuilder createSQLComandInsert(T value) throws SQLException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("INSERT INTO ").append(tableName(value.getClass()));
        textSQL.append(" (").append(columnsName(value.getClass(), true));
        textSQL.append(") VALUES (").append(createInterrogation(value.getClass(), true)).append(")");
        return textSQL;
    }

    private StringBuilder createSQLComandUpdate(T value) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchMethodException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("UPDATE ").append(tableName(value.getClass())).append(" SET ");
        textSQL.append(updateColumnsName(value));
        textSQL.append(createWhere(value.getClass(), " WHERE "));
        return textSQL;
    }

    private StringBuilder createSQLComandDelete(T value) throws SQLException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("DELETE FROM ").append(tableName(value.getClass()));
        textSQL.append(createWhere(value.getClass(), " WHERE "));
        return textSQL;
    }

    public int save(T value, String situacao) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchMethodException {
        switch (situacao) {
            case "insert":
                return insert(value);
            case "update":
                return update(value);
            case "delete":
                return delete(value);
        }
        return 0;
    }

    private int insert(T value) throws SQLException, IllegalAccessException {
        StringBuilder textoSQL = createSQLComandInsert(value);
        return percorrerObjetoInsert(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoInsert(T value, StringBuilder textoSQL) throws SQLException, IllegalAccessException {
        statement = connection.prepareStatement(textoSQL.toString());
        int cont = 0;
        percorrerObjetoInsert(value, value.getClass(), false, cont);
        return statement;
    }

    private int percorrerObjetoInsert(T value, Class<?> clazz, boolean isTabelaExterna, int cont) throws SQLException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (isTabelaExterna) {
                if (field.isAnnotationPresent(Coluna.class)) {
                    Coluna coluna = field.getAnnotation(Coluna.class);
                    if (!coluna.chavePrimaria()) {
                        continue;
                    }
                }
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    continue;
                }
            }
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class) && !isTabelaExterna) {
                cont = percorrerObjetoInsert((T) field.get(value), field.getType(), true, cont);
                field.setAccessible(false);
                continue;
            }
            if (field.isAnnotationPresent(Coluna.class)) {
                Coluna coluna = field.getAnnotation(Coluna.class);
                if (!coluna.inserivel()) {
                    field.setAccessible(false);
                    continue;
                }
                if (isTabelaExterna) {
                    if (!coluna.chavePrimaria()) {
                        field.setAccessible(false);
                        continue;
                    }

                    CreateStatementObjectValue.TypeValue(field.get(value), statement, ++cont, connection);
                    field.setAccessible(false);
                    continue;
                }
                CreateStatementObjectValue.TypeValue(field.get(value), statement, ++cont, connection);
                field.setAccessible(false);
                continue;
            }
        }
        return cont;
    }

    private int update(T value) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchMethodException {
        StringBuilder textoSQL = createSQLComandUpdate(value);
        return percorrerObjetoUpdate(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoUpdate(T value, StringBuilder textoSQL) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        PreparedStatement statement = connection.prepareStatement(textoSQL.toString());
        int cont = percorrerObjetoUpdate(value, value.getClass(), false, statement, 0);
        preencherWhere(value, value.getClass().getDeclaredFields(), statement, false, cont);
        return statement;
    }

    private int percorrerObjetoUpdate(T value, Class<?> clazz, boolean isTabelaExterna, PreparedStatement statement, int cont) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (Field field : clazz.getDeclaredFields()) {
            if (value == null) {
                break;
            }
            if (field.isAnnotationPresent(ManyToOne.class) && !isTabelaExterna) {
                field.setAccessible(true);
                cont = percorrerObjetoUpdate((T) field.get(value), field.getType(), true, statement, cont);
                field.setAccessible(false);
                continue;
            }
            if (field.isAnnotationPresent(Coluna.class)) {
                Coluna coluna = field.getAnnotation(Coluna.class);
                if (isTabelaExterna) {
                    if (!coluna.chavePrimaria() || value == null) {
                        continue;
                    }
                    field.setAccessible(true);
                    CreateStatementObjectValue.TypeValue(cast(coluna.typeBD(), field.get(value)), statement, ++cont, connection);
                    field.setAccessible(false);
                    continue;
                }
                if (!coluna.alteravel()) {
                    continue;
                }
                field.setAccessible(true);
                CreateStatementObjectValue.TypeValue(cast(coluna.typeBD(), field.get(value)), statement, ++cont, connection);
                field.setAccessible(false);
                continue;
            }
        }
        return cont;
    }

    private Object cast(Class<?> clazz, Object value) {
        if (value == null) {
            return value;
        }
        if (clazz.getSimpleName().equalsIgnoreCase("timestamp")) {
            return (Object) new Timestamp(((Date) value).getTime());
        }
        return clazz.cast(value);
    }

    private int delete(T value) throws SQLException, IllegalAccessException {
        StringBuilder textoSQL = createSQLComandDelete(value);
        return percorrerObjetoDelete(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoDelete(T value, StringBuilder textoSQL) throws SQLException, IllegalAccessException {
        PreparedStatement statement = connection.prepareStatement(textoSQL.toString());
        int cont = 0;
        preencherWhere(value, value.getClass().getDeclaredFields(), statement, false, cont);
        return statement;
    }

    private int preencherWhere(T value, Field[] fields, PreparedStatement statement, boolean isTabelaExterna, int cont) throws IllegalAccessException, SQLException {
        for (Field field : fields) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (isTabelaExterna) {
                    continue;
                }
                field.setAccessible(true);
                cont = preencherWhere((T) field.get(value), field.getType().getDeclaredFields(), statement, true, cont);
                field.setAccessible(false);
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (coluna == null || !coluna.chavePrimaria()) {
                continue;
            }
            field.setAccessible(true);
            CreateStatementObjectValue.TypeValue(field.get(value), statement, ++cont, connection);
            field.setAccessible(false);
        }
        return cont;
    }

    private String tableName(Class<?> clazz) throws SQLException {
        Tabela tabela = clazz.getAnnotation(Tabela.class);
        if (tabela == null || tabela.value() == null || tabela.value().isBlank()) {
            throw new SQLException("Table name can't defined");
        }
        return tabela.value();
    }

    private StringBuilder columnsName(Class<?> clazz, boolean insert) throws SQLException {
        return columnsName(clazz, insert, "");
    }

    private StringBuilder columnsName(Coluna coluna, boolean insert, String delimitador, StringBuilder columns) throws SQLException {
        if (coluna == null) {
            throw new SQLException("Colum can't defined");
        }
        if (coluna.chavePrimaria()) {
            columns.append(delimitador).append(coluna.nome());
            return columns;
        }
        if ((insert && !coluna.inserivel())
                || (!insert && !coluna.alteravel())) {
            return columns;
        }
        columns.append(delimitador).append(coluna.nome());
        return columns;
    }

    private StringBuilder columnsName(Class<?> clazz, boolean insert, String delimitador) throws SQLException {
        StringBuilder columns = new StringBuilder();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                    columns.append(delimitador).append(Arrays.stream(joinColumn.value()).reduce((a, b) -> a + ", " + b).get());
                    delimitador = ", ";
                    continue;
                }
                columns.append(delimitador).append(columnsName(field.getType(), insert, delimitador));
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            columns = columnsName(coluna, insert, delimitador, columns);
            delimitador = ", ";
//            columns.append(delimitador).append(StringOperations.join(field.getName()));
//            delimitador = ", ";
        }
        return columns;
    }

    private StringBuilder createInterrogation(Class<?> clazz, boolean insert) {
        StringBuilder integorration = new StringBuilder();
        String delimitador = "";
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                    integorration.append(delimitador).append(Arrays.stream(joinColumn.value()).map(x -> "?").reduce((a, b) -> a + b).get());
                    delimitador = ", ";
                    continue;
                }
                integorration.append(delimitador).append(createInterrogation(field.getType(), insert));
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            if ((insert && !coluna.inserivel())
                    || (!insert && !coluna.alteravel())) {
                continue;
            }
            integorration.append(delimitador).append("?");
            delimitador = ", ";
        }
        return integorration;
    }

    private StringBuilder createWhere(Class<?> clazz, String delimitador) {
        StringBuilder integorration = new StringBuilder();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                    for (String value : joinColumn.value()) {
                        integorration.append(delimitador).append(value).append(" = ? ");
                        delimitador = " AND ";
                    }
                    continue;
                }
                integorration.append(delimitador).append(createWhere(field.getType(), " AND "));
                delimitador = " AND ";
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (!coluna.chavePrimaria()) {
                continue;
            }
            integorration.append(delimitador).append(coluna.nome()).append(" = ? ");
            delimitador = " AND ";
        }
        return integorration;
    }

    private StringBuilder updateColumnsName(T value) throws SQLException, IllegalAccessException, NoSuchMethodException, NoSuchMethodException {
        StringBuilder textoSQL = new StringBuilder();
        String delimitador = "";
        for (Field field : value.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                field.setAccessible(true);

                int cont = 0;
                for (Field fieldMany : field.getType().getDeclaredFields()) {
                    if (!fieldMany.isAnnotationPresent(Coluna.class)) {
                        continue;
                    }
                    Coluna coluna = fieldMany.getAnnotation(Coluna.class);
                    if (!coluna.alteravel() || !verificarAtributosModificados(field.get(value), coluna.nome())) {
                        continue;
                    }
                    if (field.isAnnotationPresent(JoinColumn.class)) {
                        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                        textoSQL.append(delimitador).append(joinColumn.value()[cont++]).append(" = ? ");
                        delimitador = ", ";
                        ;
                        continue;
                    }
                    textoSQL.append(delimitador).append(coluna.nome()).append(" = ? ");
                    delimitador = ", ";
                }
                field.setAccessible(false);
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (coluna != null && coluna.alteravel()) {
                field.setAccessible(true);
                if (verificarAtributosModificados(value, coluna.nome())) {
                    textoSQL = columnsName(coluna, false, delimitador, textoSQL).append(" = ?");
                    delimitador = ", ";
                }
                field.setAccessible(false);
//                continue;
            }

        }
        return textoSQL;
    }
}

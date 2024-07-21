package br.com.mike.data.operations;

import br.com.mike.annotation.Map;
import br.com.mike.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class CRUDManager<T> {

    private final Connection connection;
    private PreparedStatement statement;

    public CRUDManager(Connection connection) {
        this.connection = connection;
    }

    private StringBuilder createSQLComandInsert(T value) throws SQLException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("INSERT INTO ").append(schemaName(value.getClass())).append(tableName(value.getClass()));
        textSQL.append(" (").append(columnsName(value.getClass(), true));
        textSQL.append(") VALUES (").append(createInterrogation(value.getClass(), true)).append(")");
        return textSQL;
    }

    private StringBuilder createSQLComandUpdate(T value) throws SQLException, IllegalAccessException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("UPDATE ").append(schemaName(value.getClass())).append(tableName(value.getClass())).append(" SET ");
        textSQL.append(updateColumnsName(value));
        textSQL.append(createWhere(value, " WHERE "));
        return textSQL;
    }

    private StringBuilder createSQLComandDelete(T value) throws SQLException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("DELETE FROM ").append(schemaName(value.getClass())).append(tableName(value.getClass()));
        textSQL.append(createWhere(value, " WHERE "));
        return textSQL;
    }

    private String schemaName(Class<?> clazz) {
        Schema schema = clazz.getAnnotation(Schema.class);
        if (schema == null || schema.value() == null) {
            return "";
        }
        return schema.value();
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
                    columns.append(delimitador).append(Arrays.stream(joinColumn.value()).map(Map::value).reduce((a, b) -> a + ", " + b).get());
                    delimitador = ", ";
                    continue;
                }
                columns.append(delimitador).append(columnsName(field.getType(), insert, delimitador));
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            columns = columnsName(coluna, insert, delimitador, columns);
            delimitador = ", ";
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

    private StringBuilder createWhere(Object value, String delimitador) {
        StringBuilder integorration = new StringBuilder();
        for (Field field : declaredListFieldsFiltredPK(value, false)) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                    for (Map map : joinColumn.value()) {
                        integorration.append(delimitador).append(map.value()).append(" = ? ");
                        delimitador = " AND ";
                    }
                    field.setAccessible(false);
                }
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

    private StringBuilder updateColumnsName(Object value) throws SQLException {
        StringBuilder textoSQL = new StringBuilder();
        String delimitador = "";
        for (Field field : declaredFields(value)) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    Map[] maps = field.getAnnotation(JoinColumn.class).value();
                    for (Map map : maps) {
                        textoSQL.append(delimitador).append(map.value()).append(" = ? ");
                        delimitador = ", ";
                    }
                }
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (coluna != null && coluna.alteravel()) {
                textoSQL = columnsName(coluna, false, delimitador, textoSQL).append(" = ?");
                delimitador = ", ";
            }
        }
        return textoSQL;
    }

    public int save(T value, String situacao) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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

    private int update(T value) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        StringBuilder textoSQL = createSQLComandUpdate(value);
        return percorrerObjetoUpdate(value, textoSQL).executeUpdate();
    }

    private int delete(T value) throws SQLException, IllegalAccessException {
        StringBuilder textoSQL = createSQLComandDelete(value);
        return percorrerObjetoDelete(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoInsert(T value, StringBuilder textoSQL) throws SQLException, IllegalAccessException {
        statement = connection.prepareStatement(textoSQL.toString());
        int cont = 0;
        percorrerObjetoInsert(value, false, cont);
        return statement;
    }

    private int percorrerObjetoInsert(Object value, boolean isTabelaExterna, int cont) throws SQLException, IllegalAccessException {
        for (Field field : isTabelaExterna ? declaredListFieldsFiltredPK(value, isTabelaExterna) : declaredFields(value)) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class) && !isTabelaExterna) {
                cont = percorrerObjetoInsert(field.get(value), true, cont);
                field.setAccessible(false);
                continue;
            }
            if (field.isAnnotationPresent(Coluna.class)) {
                Coluna coluna = field.getAnnotation(Coluna.class);
                if (isTabelaExterna) {
                    CreateStatementObjectValue.TypeValue(cast(coluna.typeBD(), field.get(value)), statement, ++cont, connection);
                    field.setAccessible(false);
                    continue;
                }
                if (!coluna.inserivel()) {
                    field.setAccessible(false);
                    continue;
                }
                CreateStatementObjectValue.TypeValue(cast(coluna.typeBD(), field.get(value)), statement, ++cont, connection);
                field.setAccessible(false);
            }
        }
        return cont;
    }

    private PreparedStatement percorrerObjetoUpdate(T value, StringBuilder textoSQL) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        statement = connection.prepareStatement(textoSQL.toString());
        int cont = percorrerObjetoUpdate(value, false, 0);
        preencherWhere(value, false, cont);
        return statement;
    }

    private int percorrerObjetoUpdate(Object value, boolean isTabelaExterna, int cont) throws SQLException, IllegalAccessException {
        for (Field field : isTabelaExterna ? declaredListFieldsFiltredPK(value, isTabelaExterna) : declaredFields(value)) {
            if (value == null) {
                break;
            }
            if (field.isAnnotationPresent(ManyToOne.class) && !isTabelaExterna) {
                field.setAccessible(true);
                cont = percorrerObjetoUpdate(field.get(value), true, cont);
                field.setAccessible(false);
                continue;
            }
            if (field.isAnnotationPresent(Coluna.class)) {
                Coluna coluna = field.getAnnotation(Coluna.class);
                if (isTabelaExterna) {
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
            }
        }
        return cont;
    }

    private PreparedStatement percorrerObjetoDelete(T value, StringBuilder textoSQL) throws SQLException, IllegalAccessException {
        statement = connection.prepareStatement(textoSQL.toString());
        int cont = 0;
        preencherWhere(value, false, cont);
        return statement;
    }

    private int preencherWhere(Object value, boolean isTabelaExterna, int cont) throws IllegalAccessException, SQLException {
        for (Field field : declaredListFieldsFiltredPK(value, isTabelaExterna)) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (isTabelaExterna) {
                    continue;
                }
                field.setAccessible(true);
                cont = preencherWhere(field.get(value), true, cont);
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

    private Object cast(Class<?> clazz, Object value) {
        if (value == null) {
            return value;
        }
        if (clazz.getSimpleName().equalsIgnoreCase("timestamp")) {
            return new Timestamp(((Date) value).getTime());
        } else if (clazz.getSimpleName().equalsIgnoreCase("uuid")) {
            return UUID.fromString((String) value);
        }
        return clazz.cast(value);
    }

    private List<Field> declaredFields(Object value) {
        return Arrays.asList(value.getClass().getDeclaredFields());
    }

    private List<Field> declaredFields(Class<?> clazz) {
        return Arrays.asList(clazz.getDeclaredFields());
    }

    private List<Field> declaredListFieldsFiltredPK(Object value, boolean isManyToOne) {
        List<Field> fields = new ArrayList<>();
        if (!isManyToOne) {
            for (Field field : declaredFields(value)) {
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    fields.add(field);
                }
            }
        }
        fields.addAll(filterListPK(declaredFields(value)));
        return fields;
    }

    private List<Field> filterListPK(List<Field> fields) {
        List<Field> fieldsFiltred = new ArrayList<>();
        for (Field field : fields) {
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (coluna != null && coluna.chavePrimaria()) {
                fieldsFiltred.add(field);
            }
        }
        return fieldsFiltred;
    }
}

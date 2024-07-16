package br.com.mike.data.operations;

import br.com.mike.annotation.Coluna;
import br.com.mike.annotation.ManyToOne;
import br.com.mike.annotation.Tabela;
import br.com.mike.annotation.JoinColumn;
import br.com.mike.comum.StringOperations;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class CRUDManager<T> {

    private StringBuilder createSQLComandInsert(T value) throws Exception {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("INSERT INTO ").append(tableName(value.getClass()));
        textSQL.append(" (").append(columnsName(value.getClass(), true));
        textSQL.append(") VALUES (").append(createInterrogation(value.getClass(), true)).append(")");
        return textSQL;
    }

    private StringBuilder createSQLComandUpdate(T value) throws SQLException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("UPDATE ").append(tableName(value.getClass())).append(" SET ");
        textSQL.append(updateColumnsName(value));
        textSQL.append(createWhere(value.getClass()));
        return textSQL;
    }

    private StringBuilder createSQLComandDelete(T value) throws SQLException {
        StringBuilder textSQL = new StringBuilder();
        textSQL.append("DELETE FROM ").append(tableName(value.getClass()));
        textSQL.append(createWhere(value.getClass()));
        return textSQL;
    }

    public int save(T value) throws SQLException, IllegalAccessException {
        switch (((ObjetoNegocio) value).getSituacao()){
            case Novo:
                return insert(value);
            case Modificado:
                return update(value);
            case Excluido:
                return delete(value);
        }
        return 0;
    }

    private int insert(T value) throws SQLException, IllegalAccessException {
        StringBuilder textoSQL = createSQLComandInsert(value);
        return percorrerObjetoInsert(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoInsert(T value, StringBuilder textoSQL)throws SQLException, IllegalAccessException{
        statement = comando.prepareStatement(textoSQL.toString());
        int cont = 0;
        percorrerObjetoInsert(value, value.getClass(), false, cont);
        return statement;
    }

    private int percorrerObjetoInsert(T value, Class<?> clazz, boolean isTabelaExterna, int cont) throws SQLException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        for(Field field: fields){
            if(isTabelaExterna){
                if(field.isAnnotationPresent(Coluna.class)){
                    Coluna coluna = field.getAnnotation(Coluna.class);
                    if(!coluna.chavePrimaria()){
                        continue;
                    }
                }
                if(field.isAnnotationPresent(ManyToOne.class)){
                    continue;
                }
            }
            field.setAccessible(true);
            if(field.isAnnotationPresent(ManyToOne.class) && !isTabelaExterna){
                cont = percorrerObjetoInsert((T) field.get(value), field.getType(), true, cont);
                field.setAccessible(false);
                continue;
            }
            if(field.isAnnotationPresent(Coluna.class)){
                Coluna coluna = field.getAnnotation(Coluna.class);
                if(!coluna.inserivel()){
                    field.setAccessible(false);
                    continue;
                }
                if(isTabelaExterna){
                    if(!coluna.chavePrimaria()){
                        field.setAccessible(false);
                        continue;
                    }
                    comando.tipoComando(field.get(value), statement, ++cont);
                    field.setAccessible(false);
                    continue;
                }
                comando.tipoComando(field.get(value), statement, ++cont);
                field.setAccessible(false);
                continue;
            }
            comando.tipoComando(field.get(value), statement, ++cont);
            field.setAccessible(false);
        }
        return cont;
    }

    private int update(T value) throws SQLException, IllegalAccessException {
        StringBuilder textoSQL = createSQLComandUpdate(value);
        return percorrerObjetoUpdate(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoUpdate(T value, StringBuilder textoSQL)throws SQLException, IllegalAccessException{
        PreparedStatement statement = comando.prepareStatement(textoSQL.toString());
        int cont = 0;
        percorrerObjetoUpdate(value, value.getClass(), false, statement, cont);
        preencherWhere(value, statement, cont);
        return statement;
    }

    private void percorrerObjetoUpdate(T value, Class<?> clazz, boolean isTabelaExterna, PreparedStatement statement, int cont) throws SQLException, IllegalAccessException {
        for(Field field: clazz.getDeclaredFields()){
            if(field.isAnnotationPresent(ManyToOne.class)){
                cont = percorrerObjetoInsert(value, field.getType(), true, cont);
                continue;
            }
            if(field.isAnnotationPresent(Coluna.class)){
                Coluna coluna = field.getAnnotation(Coluna.class);
                if(!coluna.inserivel()){
                    continue;
                }
                if(isTabelaExterna){
                    if(!coluna.chavePrimaria()){
                        continue;
                    }
                    if(((ObjetoNegocio) value).getAtributosModificados().contains(coluna.nome())) {
                        comando.tipoComando(field.get(value), statement, ++cont);
                    }
                    continue;
                }
                if(((ObjetoNegocio) value).getAtributosModificados().contains(coluna.nome())) {
                    comando.tipoComando(field.get(value), statement, ++cont);
                }
                continue;
            }
            if(((ObjetoNegocio) value).getAtributosModificados().contains(StringOperations.join(field.getName()))) {
                comando.tipoComando(field.get(value), statement, ++cont);
            }
        }
    }

    private int delete(T value) throws SQLException, IllegalAccessException {
        StringBuilder textoSQL = createSQLComandDelete(value);
        return percorrerObjetoDelete(value, textoSQL).executeUpdate();
    }

    private PreparedStatement percorrerObjetoDelete(T value, StringBuilder textoSQL)throws SQLException, IllegalAccessException{
        PreparedStatement statement = comando.prepareStatement(textoSQL.toString());
        int cont = 0;
        preencherWhere(value, statement, cont);
        return statement;
    }

    private void preencherWhere(T value, PreparedStatement statement, int cont) throws IllegalAccessException, SQLException {
        for (Field field : value.getClass().getDeclaredFields()) {
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (!coluna.chavePrimaria()) {
                continue;
            }
            comando.tipoComando(field.get(value), statement, ++cont);
        }
    }

    private String tableName(Class<?> clazz) throws SQLException {
        Tabela tabela = clazz.getAnnotation(Tabela.class);
        if (tabela == null || tabela.value() == null || tabela.value().isBlank()) {
            throw new SQLException("Table name can't defined");
        }
        return tabela.value();
    }

    private StringBuilder columnsName(Class<?> clazz, boolean insert) {
        return columnsName(clazz, insert, "");
    }

    private StringBuilder columnsName(Class<?> clazz, boolean insert, String delimitador) {
        StringBuilder columns = new StringBuilder();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                columns.append(delimitador).append(Arrays.stream(joinColumn.value()).reduce((a, b) -> a + ", " + b).get());
                delimitador = ", ";
                continue;
            }
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (coluna != null) {
                if (coluna.chavePrimaria()) {
                    columns.append(delimitador).append(coluna.nome());
                    delimitador = ", ";
                    continue;
                }
                if ((insert && !coluna.inserivel())
                        || (!insert && !coluna.alteravel())) {
                    continue;
                }
                columns.append(delimitador).append(coluna.nome());
                delimitador = ", ";
                continue;
            }
            columns.append(delimitador).append(StringOperations.join(field.getName()));
            delimitador = ", ";
        }
        return columns;
    }

    private StringBuilder createInterrogation(Class<?> clazz, boolean insert) {
        StringBuilder integorration = new StringBuilder();
        String delimitador = "";
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                integorration.append(delimitador).append(Arrays.stream(joinColumn.value()).map(x -> "?").reduce((a,b) -> a + b).get());
                delimitador = ", ";
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

    private StringBuilder createWhere(Class<?> clazz) {
        StringBuilder integorration = new StringBuilder();
        String delimitador = " WHERE ";
        for (Field field : clazz.getDeclaredFields()) {
            Coluna coluna = field.getAnnotation(Coluna.class);
            if (!coluna.chavePrimaria()) {
                continue;
            }
            integorration.append(delimitador).append(coluna.nome()).append("= ?");
            delimitador = ", ";
        }
        return integorration;
    }

    private StringBuilder updateColumnsName(T value){
        StringBuilder textoSQL = new StringBuilder();
        String delimitador = "";
        for(Field field: value.getClass().getDeclaredFields()){
            Coluna coluna = field.getAnnotation(Coluna.class);
            if(coluna != null && coluna.alteravel()){
                if(((ObjetoNegocio) value).getAtributosModificados().contains(coluna.nome())){
                    textoSQL.append(delimitador).append(coluna.nome()).append(" = ?");
                    delimitador = ", ";
                }
                continue;
            }
            String columName = StringOperations.join(field.getName());
            if(((ObjetoNegocio) value).getAtributosModificados().contains(columName)){
                textoSQL.append(delimitador).append(columName).append(" = ?");
                delimitador = ", ";
            }
        }
        return textoSQL;
    }
}

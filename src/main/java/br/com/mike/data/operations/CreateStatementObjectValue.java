package br.com.mike.data.operations;

import br.com.mike.comum.SQLObjectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

public class CreateStatementObjectValue {

    public static void TypeValue(Object valor, PreparedStatement comando, int cont, Connection con) throws SQLException {
        if (valor == null) {
            comando.setNull(cont, SQLObjectType.type(valor));
        } else {
            if (valor instanceof Object[]) {
                comando.setObject(cont, con.createArrayOf(SQLObjectType.typeString(((Object[]) valor)[0]), (Object[]) valor),
                        SQLObjectType.type(valor));
            } else if (valor instanceof java.util.ArrayList) {
                comando.setObject(cont, con.createArrayOf(SQLObjectType.typeString(((ArrayList<?>) valor).get(0)),
                                ((ArrayList<?>) valor).toArray()),
                        SQLObjectType.type(valor));
            } else if (valor instanceof String
                    && Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                    .matcher((String) valor).matches()) {
                UUID uuid = UUID.fromString((String) valor);
                comando.setObject(cont, uuid, SQLObjectType.type(uuid));
            } else {
                comando.setObject(cont, valor, SQLObjectType.type(valor));
            }
        }
    }
}

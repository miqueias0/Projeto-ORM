package br.com.mike;

import br.com.mike.mapper.TesteMapper;
import br.com.mike.model.Teste;

public class Main {

    public static void main(String[] args) throws Exception{
        TesteMapper.mapearObjeto(Teste.class, null);
//        PreparedStatement preparedStatement = conexao.getComando().createPreparedStatement(montarBaseSqlSelect());
//        ResultSet resultSet = preparedStatement.executeQuery();
//        List<?> list = new ArrayList<>();
//        while (resultSet.next()){
//            list.add(TesteMapper.mapearObjeto(Teste.class, resultSet));
//            TesteMapper.map.clear();
//        }
//        System.out.println(list.size());
    }
}
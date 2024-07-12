package org.example;

import org.example.model.Teste;
import org.example.model.TesteClasse;

import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) throws Exception, InvocationTargetException, InstantiationException, IllegalAccessException {
        TesteClasse<Teste> classe = new TesteClasse<>();
        classe.createObject(Teste.class);
    }
}
package org.example.model;

import org.example.annotation.NomeAlternativo;

import java.math.BigDecimal;
import java.util.Date;

public class Teste {

    @NomeAlternativo({"nome_completo", "nome", "nome_sobrenome"})
    private String nomeCompleto;
    private BigDecimal salario;
    @NomeAlternativo("data_nacimento")
    private Date dataNascimento;

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }
}

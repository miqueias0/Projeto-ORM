package br.com.mike.model;

import br.com.mike.annotation.Coluna;
import br.com.mike.annotation.Tabela;

import java.math.BigDecimal;
import java.util.Date;

@Tabela("modelo.teste")
public class Teste {

    @Coluna(nome = "nome_completo")
    private String nomeCompleto;
    @Coluna(nome = "salario")
    private BigDecimal salario;
    @Coluna(nome = "data_nascimento")
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

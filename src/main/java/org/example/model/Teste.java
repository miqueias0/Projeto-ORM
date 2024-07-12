package org.example.model;

import java.math.BigDecimal;
import java.util.Date;

public class Teste {

    private String nome;
    private Date dataNacimento;
    private BigDecimal salario;
    private String nomeSobrenomeMae;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Date getDataNacimento() {
        return dataNacimento;
    }

    public void setDataNacimento(Date dataNacimento) {
        this.dataNacimento = dataNacimento;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public String getNomeSobrenomeMae() {
        return nomeSobrenomeMae;
    }

    public void setNomeSobrenomeMae(String nomeSobrenomeMae) {
        this.nomeSobrenomeMae = nomeSobrenomeMae;
    }
}

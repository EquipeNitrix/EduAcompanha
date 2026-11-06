package br.com.eduacompanha.model;

import java.time.LocalDate;

public class AnoLetivo {
    private int ano;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    public AnoLetivo() {
    }

    public AnoLetivo(int ano, LocalDate dataInicio, LocalDate dataFim) {
        this.ano = ano;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

}

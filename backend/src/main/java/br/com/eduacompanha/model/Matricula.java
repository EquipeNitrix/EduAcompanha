package br.com.eduacompanha.model;

import java.time.LocalDate;

import br.com.eduacompanha.model.enums.StatusMatricula;

public class Matricula {
    private int idMatricula;
    private int idAluno;
    private int idTurma;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private StatusMatricula status;

    public Matricula() {
    }

    public Matricula(int idAluno, int idTurma, LocalDate dataInicio, LocalDate dataFim, StatusMatricula status) {
        this.idAluno = idAluno;
        this.idTurma = idTurma;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
    }

    public int getIdMatricula() {
        return idMatricula;
    }

    public void setIdMatricula(int idMatricula) {
        this.idMatricula = idMatricula;
    }

    public int getIdAluno() {
        return idAluno;
    }

    public void setIdAluno(int idAluno) {
        this.idAluno = idAluno;
    }

    public int getIdTurma() {
        return idTurma;
    }

    public void setIdTurma(int idTurma) {
        this.idTurma = idTurma;
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

    public StatusMatricula getStatus() {
        return status;
    }

    public void setStatus(StatusMatricula status) {
        this.status = status;
    }
}

package br.com.eduacompanha.model;

import br.com.eduacompanha.model.enums.Turno;

public class Turma {
    private int idTurma;
    private String nomeTurma;
    private Turno turno;
    private int anoLetivo;
    private String cie;

    public Turma() {
    }

    public Turma(String nomeTurma, Turno turno, int anoLetivo, String cie) {
        this.nomeTurma = nomeTurma;
        this.turno = turno;
        this.anoLetivo = anoLetivo;
        this.cie = cie;
    }

    public int getIdTurma() {
        return idTurma;
    }

    public void setIdTurma(int idTurma) {
        this.idTurma = idTurma;
    }

    public String getNomeTurma() {
        return nomeTurma;
    }

    public void setNomeTurma(String nomeTurma) {
        this.nomeTurma = nomeTurma;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public int getAnoLetivo() {
        return anoLetivo;
    }

    public void setAnoLetivo(int anoLetivo) {
        this.anoLetivo = anoLetivo;
    }

    public String getCie() {
        return cie;
    }

    public void setCie(String cie) {
        this.cie = cie;
    }

}

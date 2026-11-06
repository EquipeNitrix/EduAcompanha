package br.com.eduacompanha.model;

public class Responsavel {
    private int idResponsavel;
    private String nomeResponsavel;
    private String parentesco;
    private int idAluno;

    public Responsavel() {
    }

    public Responsavel(String nomeResponsavel, String parentesco, int idAluno) {
        this.nomeResponsavel = nomeResponsavel;
        this.parentesco = parentesco;
        this.idAluno = idAluno;
    }

    public int getIdResponsavel() {
        return idResponsavel;
    }

    public void setIdResponsavel(int idResponsavel) {
        this.idResponsavel = idResponsavel;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public String getParentesco() {
        return parentesco;
    }

    public void setParentesco(String parentesco) {
        this.parentesco = parentesco;
    }

    public int getIdAluno() {
        return idAluno;
    }

    public void setIdAluno(int idAluno) {
        this.idAluno = idAluno;
    }
}

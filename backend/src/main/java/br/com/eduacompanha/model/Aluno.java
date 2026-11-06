package br.com.eduacompanha.model;

public class Aluno {
    private int idAluno;
    private String ra;
    private String nomeAluno;

    public Aluno() {
    }

    public Aluno(String ra, String nomeAluno) {
        this.ra = ra;
        this.nomeAluno = nomeAluno;
    }

    public int getIdAluno() {
        return idAluno;
    }

    public void setIdAluno(int idAluno) {
        this.idAluno = idAluno;
    }

    public String getRa() {
        return ra;
    }

    public void setRa(String ra) {
        this.ra = ra;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public void setNomeAluno(String nomeAluno) {
        this.nomeAluno = nomeAluno;
    }
}

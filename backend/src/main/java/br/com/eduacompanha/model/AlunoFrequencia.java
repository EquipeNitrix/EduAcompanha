package br.com.eduacompanha.model;

public class AlunoFrequencia {
    private Aluno aluno;
    private Turma turma;
    private int aulasPrevistas;
    private int aulasComFalta;
    private int diasComFalta;
    private Double percentual;

    public AlunoFrequencia() {
    }

    public AlunoFrequencia(Aluno aluno, Turma turma, int aulasPrevistas, int aulasComFalta, int diasComFalta,
            Double percentual) {
        this.aluno = aluno;
        this.turma = turma;
        this.aulasPrevistas = aulasPrevistas;
        this.aulasComFalta = aulasComFalta;
        this.diasComFalta = diasComFalta;
        this.percentual = percentual;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public void setAluno(Aluno aluno) {
        this.aluno = aluno;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    public int getAulasPrevistas() {
        return aulasPrevistas;
    }

    public void setAulasPrevistas(int aulasPrevistas) {
        this.aulasPrevistas = aulasPrevistas;
    }

    public int getAulasComFalta() {
        return aulasComFalta;
    }

    public void setAulasComFalta(int aulasComFalta) {
        this.aulasComFalta = aulasComFalta;
    }

    public int getDiasComFalta() {
        return diasComFalta;
    }

    public void setDiasComFalta(int diasComFalta) {
        this.diasComFalta = diasComFalta;
    }

    public Double getPercentual() {
        return percentual;
    }

    public void setPercentual(Double percentual) {
        this.percentual = percentual;
    }

}

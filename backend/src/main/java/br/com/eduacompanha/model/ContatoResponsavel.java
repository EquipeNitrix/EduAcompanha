package br.com.eduacompanha.model;

public class ContatoResponsavel {
    private int idContato;
    private String telefone;
    private int posicao;
    private int idResponsavel;

    public ContatoResponsavel() {
    }

    public ContatoResponsavel(String telefone, int posicao, int idResponsavel) {
        this.telefone = telefone;
        this.posicao = posicao;
        this.idResponsavel = idResponsavel;
    }

    public int getIdContato() {
        return idContato;
    }

    public void setIdContato(int idContato) {
        this.idContato = idContato;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public int getIdResponsavel() {
        return idResponsavel;
    }

    public void setIdResponsavel(int idResponsavel) {
        this.idResponsavel = idResponsavel;
    }

}

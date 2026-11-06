package br.com.eduacompanha.model;

import java.time.LocalDateTime;

public class EnvioNotificacao {
    private int idEnvio;
    private int idNotificacao;
    private Integer idContato;
    private int posicao;
    private String telefoneEnviado;
    private LocalDateTime enviadoEm;
    private boolean entregue;

    public EnvioNotificacao() {
    }

    public EnvioNotificacao(int idNotificacao, Integer idContato, int posicao, String telefoneEnviado,
            LocalDateTime enviadoEm, boolean entregue) {
        this.idNotificacao = idNotificacao;
        this.idContato = idContato;
        this.posicao = posicao;
        this.telefoneEnviado = telefoneEnviado;
        this.enviadoEm = enviadoEm;
        this.entregue = entregue;
    }

    public int getIdEnvio() {
        return idEnvio;
    }

    public void setIdEnvio(int idEnvio) {
        this.idEnvio = idEnvio;
    }

    public int getIdNotificacao() {
        return idNotificacao;
    }

    public void setIdNotificacao(int idNotificacao) {
        this.idNotificacao = idNotificacao;
    }

    public Integer getIdContato() {
        return idContato;
    }

    public void setIdContato(Integer idContato) {
        this.idContato = idContato;
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public String getTelefoneEnviado() {
        return telefoneEnviado;
    }

    public void setTelefoneEnviado(String telefoneEnviado) {
        this.telefoneEnviado = telefoneEnviado;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public boolean isEntregue() {
        return entregue;
    }

    public void setEntregue(boolean entregue) {
        this.entregue = entregue;
    }
}

package br.com.eduacompanha.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.eduacompanha.model.enums.StatusNotificacao;

public class Notificacao {
    private int idNotificacao;
    private int idMatricula;
    private LocalDate dataFalta;
    private String mensagem;
    private StatusNotificacao status;
    private String resposta;
    private LocalDateTime respondidaEm;
    private LocalDateTime encerradaEm;
    private String observacao;

    public Notificacao() {
    }

    public Notificacao(int idMatricula, LocalDate dataFalta, String mensagem, StatusNotificacao status, String resposta,
            LocalDateTime respondidaEm, LocalDateTime encerradaEm, String observacao) {
        this.idMatricula = idMatricula;
        this.dataFalta = dataFalta;
        this.mensagem = mensagem;
        this.status = status;
        this.resposta = resposta;
        this.respondidaEm = respondidaEm;
        this.encerradaEm = encerradaEm;
        this.observacao = observacao;
    }

    public int getIdNotificacao() {
        return idNotificacao;
    }

    public void setIdNotificacao(int idNotificacao) {
        this.idNotificacao = idNotificacao;
    }

    public int getIdMatricula() {
        return idMatricula;
    }

    public void setIdMatricula(int idMatricula) {
        this.idMatricula = idMatricula;
    }

    public LocalDate getDataFalta() {
        return dataFalta;
    }

    public void setDataFalta(LocalDate dataFalta) {
        this.dataFalta = dataFalta;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public StatusNotificacao getStatus() {
        return status;
    }

    public void setStatus(StatusNotificacao status) {
        this.status = status;
    }

    public String getResposta() {
        return resposta;
    }

    public void setResposta(String resposta) {
        this.resposta = resposta;
    }

    public LocalDateTime getRespondidaEm() {
        return respondidaEm;
    }

    public void setRespondidaEm(LocalDateTime respondidaEm) {
        this.respondidaEm = respondidaEm;
    }

    public LocalDateTime getEncerradaEm() {
        return encerradaEm;
    }

    public void setEncerradaEm(LocalDateTime encerradaEm) {
        this.encerradaEm = encerradaEm;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

}

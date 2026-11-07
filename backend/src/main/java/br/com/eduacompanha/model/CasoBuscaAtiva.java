package br.com.eduacompanha.model;

import java.time.LocalDate;

import br.com.eduacompanha.model.enums.StatusCaso;

public class CasoBuscaAtiva {
    private int idCaso;
    private LocalDate dataAbertura;
    private StatusCaso status;
    private LocalDate dataEncerramento;
    private int idAluno;
    private String cie;
    private String motivo;
    private String desfecho;
    private String observacaoFinal;
    private int idUsuarioAbertura;

    public CasoBuscaAtiva() {
    }

    public CasoBuscaAtiva(LocalDate dataAbertura, StatusCaso status, LocalDate dataEncerramento, int idAluno,
            String cie, String motivo, String desfecho, String observacaoFinal, int idUsuarioAbertura) {
        this.dataAbertura = dataAbertura;
        this.status = status;
        this.dataEncerramento = dataEncerramento;
        this.idAluno = idAluno;
        this.cie = cie;
        this.motivo = motivo;
        this.desfecho = desfecho;
        this.observacaoFinal = observacaoFinal;
        this.idUsuarioAbertura = idUsuarioAbertura;
    }

    public int getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(int idCaso) {
        this.idCaso = idCaso;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDate dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public StatusCaso getStatus() {
        return status;
    }

    public void setStatus(StatusCaso status) {
        this.status = status;
    }

    public LocalDate getDataEncerramento() {
        return dataEncerramento;
    }

    public void setDataEncerramento(LocalDate dataEncerramento) {
        this.dataEncerramento = dataEncerramento;
    }

    public int getIdAluno() {
        return idAluno;
    }

    public void setIdAluno(int idAluno) {
        this.idAluno = idAluno;
    }

    public String getCie() {
        return cie;
    }

    public void setCie(String cie) {
        this.cie = cie;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDesfecho() {
        return desfecho;
    }

    public void setDesfecho(String desfecho) {
        this.desfecho = desfecho;
    }

    public String getObservacaoFinal() {
        return observacaoFinal;
    }

    public void setObservacaoFinal(String observacaoFinal) {
        this.observacaoFinal = observacaoFinal;
    }

    public int getIdUsuarioAbertura() {
        return idUsuarioAbertura;
    }

    public void setIdUsuarioAbertura(int idUsuarioAbertura) {
        this.idUsuarioAbertura = idUsuarioAbertura;
    }

}

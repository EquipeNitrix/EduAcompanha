package br.com.eduacompanha.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import br.com.eduacompanha.model.enums.StatusRegistro;
import br.com.eduacompanha.model.enums.TipoOcorrencia;

public class RegistroFrequencia {
    private int idRegistroFreq;
    private LocalDate data;
    private TipoOcorrencia tipoOcorrencia;
    private int idMatricula;
    private LocalDateTime registradoEm;
    private int idUsuarioRegistro;
    private StatusRegistro status;
    private String motivoCancelamento;
    private LocalDateTime canceladoEm;
    private Integer idUsuarioCancelamento;
    private LocalTime horaEntrada;
    private LocalTime horaSaida;
    private String observacao;

    public RegistroFrequencia() {
    }

    public RegistroFrequencia(LocalDate data, TipoOcorrencia tipoOcorrencia, int idMatricula,
            LocalDateTime registradoEm,
            int idUsuarioRegistro, StatusRegistro status, String motivoCancelamento, LocalDateTime canceladoEm,
            Integer idUsuarioCancelamento, LocalTime horaEntrada, LocalTime horaSaida, String observacao) {
        this.data = data;
        this.tipoOcorrencia = tipoOcorrencia;
        this.idMatricula = idMatricula;
        this.registradoEm = registradoEm;
        this.idUsuarioRegistro = idUsuarioRegistro;
        this.status = status;
        this.motivoCancelamento = motivoCancelamento;
        this.canceladoEm = canceladoEm;
        this.idUsuarioCancelamento = idUsuarioCancelamento;
        this.horaEntrada = horaEntrada;
        this.horaSaida = horaSaida;
        this.observacao = observacao;
    }

    public int getIdRegistroFreq() {
        return idRegistroFreq;
    }

    public void setIdRegistroFreq(int idRegistroFreq) {
        this.idRegistroFreq = idRegistroFreq;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public TipoOcorrencia getTipoOcorrencia() {
        return tipoOcorrencia;
    }

    public void setTipoOcorrencia(TipoOcorrencia tipoOcorrencia) {
        this.tipoOcorrencia = tipoOcorrencia;
    }

    public int getIdMatricula() {
        return idMatricula;
    }

    public void setIdMatricula(int idMatricula) {
        this.idMatricula = idMatricula;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }

    public int getIdUsuarioRegistro() {
        return idUsuarioRegistro;
    }

    public void setIdUsuarioRegistro(int idUsuarioRegistro) {
        this.idUsuarioRegistro = idUsuarioRegistro;
    }

    public StatusRegistro getStatus() {
        return status;
    }

    public void setStatus(StatusRegistro status) {
        this.status = status;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public void setCanceladoEm(LocalDateTime canceladoEm) {
        this.canceladoEm = canceladoEm;
    }

    public Integer getIdUsuarioCancelamento() {
        return idUsuarioCancelamento;
    }

    public void setIdUsuarioCancelamento(Integer idUsuarioCancelamento) {
        this.idUsuarioCancelamento = idUsuarioCancelamento;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public LocalTime getHoraSaida() {
        return horaSaida;
    }

    public void setHoraSaida(LocalTime horaSaida) {
        this.horaSaida = horaSaida;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

}

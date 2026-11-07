package br.com.eduacompanha.model;

import java.time.LocalDateTime;

import br.com.eduacompanha.model.enums.TipoRegistro;

public class AcompanhamentoCaso {
    private int idAcompanhamento;
    private TipoRegistro tipoRegistro;
    private LocalDateTime dataHora;
    private String descricao;
    private String canal;
    private String resultado;
    private String tipoAcao;
    private int idCaso;
    private Integer idResponsavel;
    private int idUsuario;

    public AcompanhamentoCaso() {
    }

    public AcompanhamentoCaso(TipoRegistro tipoRegistro, LocalDateTime dataHora, String descricao, String canal,
            String resultado, String tipoAcao, int idCaso, Integer idResponsavel, int idUsuario) {
        this.tipoRegistro = tipoRegistro;
        this.dataHora = dataHora;
        this.descricao = descricao;
        this.canal = canal;
        this.resultado = resultado;
        this.tipoAcao = tipoAcao;
        this.idCaso = idCaso;
        this.idResponsavel = idResponsavel;
        this.idUsuario = idUsuario;
    }

    public int getIdAcompanhamento() {
        return idAcompanhamento;
    }

    public void setIdAcompanhamento(int idAcompanhamento) {
        this.idAcompanhamento = idAcompanhamento;
    }

    public TipoRegistro getTipoRegistro() {
        return tipoRegistro;
    }

    public void setTipoRegistro(TipoRegistro tipoRegistro) {
        this.tipoRegistro = tipoRegistro;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getTipoAcao() {
        return tipoAcao;
    }

    public void setTipoAcao(String tipoAcao) {
        this.tipoAcao = tipoAcao;
    }

    public int getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(int idCaso) {
        this.idCaso = idCaso;
    }

    public Integer getIdResponsavel() {
        return idResponsavel;
    }

    public void setIdResponsavel(Integer idResponsavel) {
        this.idResponsavel = idResponsavel;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

}

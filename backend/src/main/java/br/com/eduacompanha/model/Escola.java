package br.com.eduacompanha.model;

import java.time.LocalTime;

import br.com.eduacompanha.model.enums.ModeloTurno;
import br.com.eduacompanha.model.enums.Turno;

public class Escola {
    private String cie;
    private String nomeEscola;
    private String municipio;
    private ModeloTurno modeloTurno;
    private LocalTime horarioEnvioManha;
    private LocalTime horarioEnvioTarde;
    private LocalTime horarioEnvioNoite;
    private LocalTime horarioEnvioIntegral;

    public Escola() {
    }

    public Escola(String cie, String nomeEscola, String municipio, ModeloTurno modeloTurno, LocalTime horarioEnvioManha,
            LocalTime horarioEnvioTarde, LocalTime horarioEnvioNoite, LocalTime horarioEnvioIntegral) {
        this.cie = cie;
        this.nomeEscola = nomeEscola;
        this.municipio = municipio;
        this.modeloTurno = modeloTurno;
        this.horarioEnvioManha = horarioEnvioManha;
        this.horarioEnvioTarde = horarioEnvioTarde;
        this.horarioEnvioNoite = horarioEnvioNoite;
        this.horarioEnvioIntegral = horarioEnvioIntegral;
    }

    public String getCie() {
        return cie;
    }

    public void setCie(String cie) {
        this.cie = cie;
    }

    public String getNomeEscola() {
        return nomeEscola;
    }

    public void setNomeEscola(String nomeEscola) {
        this.nomeEscola = nomeEscola;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public ModeloTurno getModeloTurno() {
        return modeloTurno;
    }

    public void setModeloTurno(ModeloTurno modeloTurno) {
        this.modeloTurno = modeloTurno;
    }

    public LocalTime getHorarioEnvioManha() {
        return horarioEnvioManha;
    }

    public void setHorarioEnvioManha(LocalTime horarioEnvioManha) {
        this.horarioEnvioManha = horarioEnvioManha;
    }

    public LocalTime getHorarioEnvioTarde() {
        return horarioEnvioTarde;
    }

    public void setHorarioEnvioTarde(LocalTime horarioEnvioTarde) {
        this.horarioEnvioTarde = horarioEnvioTarde;
    }

    public LocalTime getHorarioEnvioNoite() {
        return horarioEnvioNoite;
    }

    public void setHorarioEnvioNoite(LocalTime horarioEnvioNoite) {
        this.horarioEnvioNoite = horarioEnvioNoite;
    }

    public LocalTime getHorarioEnvioIntegral() {
        return horarioEnvioIntegral;
    }

    public void setHorarioEnvioIntegral(LocalTime horarioEnvioIntegral) {
        this.horarioEnvioIntegral = horarioEnvioIntegral;
    }

    public LocalTime getHorarioEnvio(Turno turno) {
        switch (turno) {
            case MANHA:
                return horarioEnvioManha;
            case TARDE:
                return horarioEnvioTarde;
            case NOITE:
                return horarioEnvioNoite;
            case INTEGRAL:
                return horarioEnvioIntegral;
            default:
                return null;
        }
    }

    public void setHorarioEnvio(Turno turno, LocalTime horario) {
        switch (turno) {
            case MANHA:
                horarioEnvioManha = horario;
                break;
            case TARDE:
                horarioEnvioTarde = horario;
                break;
            case NOITE:
                horarioEnvioNoite = horario;
                break;
            case INTEGRAL:
                horarioEnvioIntegral = horario;
                break;
            default:
                break;
        }
    }

}
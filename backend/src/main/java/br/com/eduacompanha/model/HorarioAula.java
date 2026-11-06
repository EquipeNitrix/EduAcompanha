package br.com.eduacompanha.model;

import java.time.LocalDate;
import java.time.LocalTime;

import br.com.eduacompanha.model.enums.Turno;

public class HorarioAula {
    private int idHorario;
    private String cie;
    private Turno turno;
    private int diaSemana;
    private int ordemAula;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private LocalDate dataVigencia;

    public HorarioAula() {
    }

    public HorarioAula(String cie, Turno turno, int diaSemana, int ordemAula, LocalTime horaInicio, LocalTime horaFim,
            LocalDate dataVigencia) {
        this.cie = cie;
        this.turno = turno;
        this.diaSemana = diaSemana;
        this.ordemAula = ordemAula;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.dataVigencia = dataVigencia;
    }

    public int getIdHorario() {
        return idHorario;
    }

    public void setIdHorario(int idHorario) {
        this.idHorario = idHorario;
    }

    public String getCie() {
        return cie;
    }

    public void setCie(String cie) {
        this.cie = cie;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public int getOrdemAula() {
        return ordemAula;
    }

    public void setOrdemAula(int ordemAula) {
        this.ordemAula = ordemAula;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }

    public LocalDate getDataVigencia() {
        return dataVigencia;
    }

    public void setDataVigencia(LocalDate dataVigencia) {
        this.dataVigencia = dataVigencia;
    }

}

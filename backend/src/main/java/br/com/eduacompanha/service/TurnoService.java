package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.dao.IHorarioAulaDao;
import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.RegistroFrequencia;
import br.com.eduacompanha.model.enums.TipoOcorrencia;

public class TurnoService {
    private IHorarioAulaDao horarioAulaDao;
    private Clock relogio;

    public TurnoService(IHorarioAulaDao horarioAulaDao, Clock relogio) {
        this.horarioAulaDao = horarioAulaDao;
        this.relogio = relogio;
    }

    public List<HorarioAula> aulasDoDia(List<HorarioAula> versoes, LocalDate data) {
        if (versoes == null || versoes.isEmpty()) {
            return null;
        }

        int diaSemana = data.getDayOfWeek().getValue();
        if (diaSemana > 5) {
            return new ArrayList<>();
        }

        LocalDate vigente = null;
        for (HorarioAula h : versoes) {
            LocalDate v = h.getDataVigencia();
            if (!v.isAfter(data) && (vigente == null || v.isAfter(vigente))) {
                vigente = v;
            }
        }
        if (vigente == null) {
            return new ArrayList<>();
        }

        List<HorarioAula> aulas = new ArrayList<>();
        for (HorarioAula h : versoes) {
            if (h.getDataVigencia().equals(vigente) && h.getDiaSemana() == diaSemana) {
                aulas.add(h);
            }
        }
        return aulas;
    }

    public List<HorarioAula> aulasPerdidas(List<HorarioAula> versoes, RegistroFrequencia r) {
        List<HorarioAula> aulas = aulasDoDia(versoes, r.getData());
        if (aulas == null) {
            return null;
        }

        if (r.getTipoOcorrencia() == TipoOcorrencia.FALTA) {
            return aulas;
        }

        if (r.getTipoOcorrencia() == TipoOcorrencia.ATRASO) {
            List<HorarioAula> perdidas = new ArrayList<>();
            for (HorarioAula h : aulas) {
                if (h.getHoraInicio().isBefore(r.getHoraEntrada())) {
                    perdidas.add(h);
                }
            }
            return perdidas;
        }
    }
}

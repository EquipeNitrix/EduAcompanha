package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import br.com.eduacompanha.dao.IAnoLetivoDao;
import br.com.eduacompanha.dao.IEscolaDao;
import br.com.eduacompanha.dao.IHorarioAulaDao;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.RegistroFrequencia;
import br.com.eduacompanha.model.enums.TipoOcorrencia;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.util.SessaoUtil;

public class TurnoService {
    private IHorarioAulaDao horarioAulaDao;
    private IEscolaDao escolaDao;
    private IAnoLetivoDao anoLetivoDao;
    private Clock relogio;

    public TurnoService(IHorarioAulaDao horarioAulaDao, IEscolaDao escolaDao, IAnoLetivoDao anoLetivoDao,
            Clock relogio) {
        this.horarioAulaDao = horarioAulaDao;
        this.escolaDao = escolaDao;
        this.anoLetivoDao = anoLetivoDao;
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

        if (r.getTipoOcorrencia() == TipoOcorrencia.SAIDA_ANTECIPADA) {
            List<HorarioAula> perdidas = new ArrayList<>();
            for (HorarioAula h : aulas) {
                if (!h.getHoraInicio().isBefore(r.getHoraSaida())) {
                    perdidas.add(h);
                }
            }
            return perdidas;
        }

        if (r.getTipoOcorrencia() == TipoOcorrencia.SAIDA_COM_RETORNO) {
            List<HorarioAula> perdidas = new ArrayList<>();
            for (HorarioAula h : aulas) {
                if (!h.getHoraInicio().isBefore(r.getHoraSaida()) && h.getHoraInicio().isBefore(r.getHoraEntrada())) {
                    perdidas.add(h);
                }
            }
            return perdidas;
        }
        return new ArrayList<>();
    }

    public List<HorarioAula> versoes(String cie, Turno turno) {
        SessaoUtil.exigirAcessoEscola(cie);
        return horarioAulaDao.consultarPorEscolaTurno(cie, turno);
    }

    public boolean salvarTurno(String cie, Turno turno, LocalTime envio, List<HorarioAula> grade, LocalDate vigencia) {
        SessaoUtil.exigirAcessoEscola(cie);
        Escola escola = escolaDao.consultar(cie);
        if (escola == null) {
            throw new NaoEncontradoException("Escola não encontrada.");
        }
        if (!escola.getModeloTurno().turnos().contains(turno)) {
            throw new ValidacaoException("turno", "Este turno não faz parte do modelo de turno da escola.");
        }

        if (grade == null) {
            grade = new ArrayList<>();
        }
        for (HorarioAula a : grade) {
            if (a.getHoraInicio() == null) {
                throw new ValidacaoException("grade", "Informe o início.");
            }
            if (a.getHoraFim() == null) {
                throw new ValidacaoException("grade", "Informe o término.");
            }
            if (!a.getHoraFim().isAfter(a.getHoraInicio())) {
                throw new ValidacaoException("grade", "O término precisa ser depois do início.");
            }
        }

        for (int dia = 1; dia <= 5; dia++) {
            List<HorarioAula> doDia = new ArrayList<>();
            for (HorarioAula a : grade) {
                if (a.getDiaSemana() == dia) {
                    doDia.add(a);
                }
            }

            doDia.sort(Comparator.comparing(HorarioAula::getHoraInicio));

            if (doDia.size() > 15) {
                throw new ValidacaoException("grade", "No máximo 15 aulas por dia.");
            }

            for (int i = 0; i < doDia.size(); i++) {
                HorarioAula a = doDia.get(i);
                a.setOrdemAula(i + 1);
                if (i > 0 && a.getHoraInicio().isBefore(doDia.get(i - 1).getHoraFim())) {
                    throw new ValidacaoException("grade", "Esta aula começa antes de a anterior terminar.");
                }
            }
        }

        LocalDate hoje = LocalDate.now(relogio);
        List<HorarioAula> atuais = horarioAulaDao.consultarPorEscolaTurno(cie, turno);
        if (grade.isEmpty() && !atuais.isEmpty()) {
            throw new RegraNegocioException(
                    "Este turno já tem grade de horários e não pode ficar sem ela, porque a frequência já calculada depende dela.");
        }

        if (!grade.isEmpty()) {
            if (atuais.isEmpty()) {
                vigencia = anoLetivoDao.consultar(hoje.getYear()).getDataInicio();
            } else {
                if (vigencia == null) {
                    throw new ValidacaoException("vigencia", "Informe a partir de quando a grade nova vale.");
                }
                if (vigencia.isBefore(hoje)) {
                    throw new ValidacaoException("vigencia",
                            "Use hoje ou uma data futura. Os dias que já passaram mantêm a grade que valia.");
                }
                // TODO RN-26
            }
        }

        escola.setHorarioEnvio(turno, envio);
        escolaDao.alterar(escola, cie);
        if (grade.isEmpty()) {
            return false;
        }

        horarioAulaDao.cadastrarVersao(cie, turno, vigencia, grade);
        return true;

    }
}

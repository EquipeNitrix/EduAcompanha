package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.eduacompanha.dao.IAlunoDao;
import br.com.eduacompanha.dao.IAnoLetivoDao;
import br.com.eduacompanha.dao.IMatriculaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.AnoLetivo;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.StatusMatricula;
import br.com.eduacompanha.util.SessaoUtil;

public class MatriculaService {
    private IMatriculaDao matriculaDao;
    private IAlunoDao alunoDao;
    private ITurmaDao turmaDao;
    private IAnoLetivoDao anoLetivoDao;
    private Clock relogio;

    public MatriculaService(IMatriculaDao matriculaDao, IAlunoDao alunoDao, ITurmaDao turmaDao,
            IAnoLetivoDao anoLetivoDao, Clock relogio) {
        this.matriculaDao = matriculaDao;
        this.alunoDao = alunoDao;
        this.turmaDao = turmaDao;
        this.anoLetivoDao = anoLetivoDao;
        this.relogio = relogio;
    }

    public Matricula matricular(int idAluno, int idTurma, LocalDate inicio) {
        SessaoUtil.exigirAdmin();

        Map<String, String> erros = new LinkedHashMap<>();
        if (idAluno == 0) {
            erros.put("aluno", "Escolha o aluno.");
        }
        if (idTurma == 0) {
            erros.put("turma", "Escolha a turma.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        if (alunoDao.consultar(idAluno) == null) {
            throw new NaoEncontradoException("Aluno não encontrado.");
        }
        Turma turma = turmaDao.consultar(idTurma);
        if (turma == null) {
            throw new NaoEncontradoException("Turma não encontrada.");
        }

        int anoAtual = LocalDate.now(relogio).getYear();
        if (turma.getAnoLetivo() != anoAtual) {
            throw new RegraNegocioException("Só é possível matricular em turmas do ano letivo atual.");
        }

        AnoLetivo anoLetivo = anoLetivoDao.consultar(anoAtual);
        if (inicio == null) {
            throw new ValidacaoException("inicio", "Informe a data de início.");
        } else if (inicio.isBefore(anoLetivo.getDataInicio()) || inicio.isAfter(anoLetivo.getDataFim())) {
            DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new ValidacaoException("inicio", "Use uma data entre " + br.format(anoLetivo.getDataInicio()) + " e "
                    + br.format(anoLetivo.getDataFim()) + ".");
        }

        if (matriculaDao.consultarAtivaDoAluno(idAluno, anoLetivo.getAno()) != null) {
            throw new ValidacaoException("aluno",
                    "Este aluno já tem matrícula ativa neste ano letivo. Encerre a atual antes.");
        }

        Matricula nova = new Matricula(idAluno, idTurma, inicio, null, StatusMatricula.ATIVA);
        return matriculaDao.cadastrar(nova);
    }

    public Matricula encerrar(int idMatricula, LocalDate fim) {
        SessaoUtil.exigirAdmin();

        Matricula matricula = matriculaDao.consultar(idMatricula);
        if (matricula == null) {
            throw new NaoEncontradoException("Matrícula não encontrada.");
        }

        if (matricula.getStatus() != StatusMatricula.ATIVA) {
            throw new RegraNegocioException("Esta matrícula já está encerrada.");
        }

        if (fim == null) {
            throw new ValidacaoException("fim", "Informe a data.");
        }
        if (fim.isBefore(matricula.getDataInicio())) {
            throw new ValidacaoException("fim", "A data não pode ser anterior ao início da matrícula.");
        }

        matricula.setStatus(StatusMatricula.ENCERRADA);
        matricula.setDataFim(fim);
        matriculaDao.alterar(matricula, idMatricula);
        return matricula;
    }

    public List<Matricula> doAluno(int idAluno) {
        SessaoUtil.exigirAdmin();
        return matriculaDao.consultarPorAluno(idAluno);
    }

}

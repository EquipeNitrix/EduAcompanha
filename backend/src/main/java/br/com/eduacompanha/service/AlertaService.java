package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.eduacompanha.dao.IAnoLetivoDao;
import br.com.eduacompanha.dao.IMatriculaDao;
import br.com.eduacompanha.dao.IRegistroFrequenciaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.AlunoFrequencia;
import br.com.eduacompanha.model.AnoLetivo;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.RegistroFrequencia;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.StatusRegistro;
import br.com.eduacompanha.model.enums.TipoOcorrencia;
import br.com.eduacompanha.util.SessaoUtil;

// Alerta de evasão (RN-57 a 59). Regras copiadas do alerta.service.js do site.
// Admin vê qualquer escola; a secretaria só a dela.
public class AlertaService {

    // constantes.js do site
    public static final double LIMITE_FREQUENCIA = 75; // abaixo disso: crítico
    public static final double LIMITE_ATENCAO = 80; // de 75 até abaixo de 80: atenção
    private static final int PADRAO_JANELA_DIAS = 30;
    private static final int PADRAO_MINIMO = 3;

    private FrequenciaService frequenciaService;
    private IMatriculaDao matriculaDao;
    private ITurmaDao turmaDao;
    private IAnoLetivoDao anoLetivoDao;
    private IRegistroFrequenciaDao registroDao;
    private Clock relogio;

    public AlertaService(FrequenciaService frequenciaService, IMatriculaDao matriculaDao, ITurmaDao turmaDao,
            IAnoLetivoDao anoLetivoDao, IRegistroFrequenciaDao registroDao, Clock relogio) {
        this.frequenciaService = frequenciaService;
        this.matriculaDao = matriculaDao;
        this.turmaDao = turmaDao;
        this.anoLetivoDao = anoLetivoDao;
        this.registroDao = registroDao;
        this.relogio = relogio;
    }

    // RN-57: o nível de alerta pelo percentual. static: não precisa de banco nem de objeto.
    public static String nivel(Double percentual) {
        if (percentual == null) {
            return null; // turno sem grade: sem alerta
        }
        if (percentual < LIMITE_FREQUENCIA) {
            return "critico";
        }
        if (percentual < LIMITE_ATENCAO) {
            return "atencao";
        }
        return null;
    }

    // Todos os alunos com matrícula ativa no ano atual, com a frequência calculada.
    // idTurma null = todas as turmas da escola.
    public List<AlunoFrequencia> indicadores(String cie, Integer idTurma) {
        SessaoUtil.exigirAcessoEscola(cie);
        int anoAtual = LocalDate.now(relogio).getYear();
        List<AlunoFrequencia> lista = new ArrayList<>();
        for (Turma turma : turmaDao.consultarPorEscola(cie, anoAtual)) {
            if (idTurma != null && turma.getIdTurma() != idTurma) {
                continue;
            }
            for (Matricula m : matriculaDao.consultarAtivasDaTurma(turma.getIdTurma())) {
                lista.add(frequenciaService.frequenciaDaMatricula(m));
            }
        }
        return lista;
    }

    // RN-57: só quem está em alerta, da menor para a maior frequência.
    public List<AlunoFrequencia> alunosEmAlerta(String cie, Integer idTurma) {
        List<AlunoFrequencia> emAlerta = new ArrayList<>();
        for (AlunoFrequencia f : indicadores(cie, idTurma)) {
            if (nivel(f.getPercentual()) != null) {
                emAlerta.add(f);
            }
        }
        emAlerta.sort((a, b) -> Double.compare(a.getPercentual(), b.getPercentual()));
        return emAlerta;
    }

    // RN-58: padrão de falta no mesmo dia da semana, nos últimos 30 dias.
    public List<String> padroesDeFalta(int idAluno) {
        LocalDate hoje = LocalDate.now(relogio);
        Matricula matricula = matriculaDao.consultarAtivaDoAluno(idAluno, hoje.getYear());
        if (matricula == null) {
            return new ArrayList<>();
        }
        Turma turma = turmaDao.consultar(matricula.getIdTurma());
        SessaoUtil.exigirAcessoEscola(turma.getCie());
        AnoLetivo ano = anoLetivoDao.consultar(turma.getAnoLetivo());

        // janela: 30 dias que terminam hoje (ou no fim do ano letivo), sem passar do início da matrícula
        LocalDate fim = hoje.isBefore(ano.getDataFim()) ? hoje : ano.getDataFim();
        LocalDate inicio = fim.minusDays(PADRAO_JANELA_DIAS - 1);
        if (matricula.getDataInicio().isAfter(inicio)) {
            inicio = matricula.getDataInicio();
        }

        // faltas ativas dentro da janela
        List<RegistroFrequencia> faltas = new ArrayList<>();
        for (RegistroFrequencia r : registroDao.consultarPorMatricula(matricula.getIdMatricula())) {
            if (r.getStatus() == StatusRegistro.ATIVO && r.getTipoOcorrencia() == TipoOcorrencia.FALTA
                    && !r.getData().isBefore(inicio) && !r.getData().isAfter(fim)) {
                faltas.add(r);
            }
        }
        if (faltas.size() < PADRAO_MINIMO) {
            return new ArrayList<>();
        }

        // conta as faltas por dia da semana (1 = segunda ... 5 = sexta)
        Map<Integer, Integer> porDia = new TreeMap<>();
        for (RegistroFrequencia r : faltas) {
            int dia = r.getData().getDayOfWeek().getValue();
            porDia.put(dia, porDia.getOrDefault(dia, 0) + 1);
        }

        String[] plural = { "", "segundas-feiras", "terças-feiras", "quartas-feiras", "quintas-feiras",
                "sextas-feiras", "sábados", "domingos" };
        List<String> padroes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : porDia.entrySet()) {
            if (e.getValue() >= PADRAO_MINIMO) {
                padroes.add("Faltou em " + e.getValue() + " " + plural[e.getKey()] + " nos últimos "
                        + PADRAO_JANELA_DIAS + " dias");
            }
        }
        return padroes;
    }
}

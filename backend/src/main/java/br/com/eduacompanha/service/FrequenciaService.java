package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.eduacompanha.dao.IAlunoDao;
import br.com.eduacompanha.dao.IAnoLetivoDao;
import br.com.eduacompanha.dao.IMatriculaDao;
import br.com.eduacompanha.dao.IRegistroFrequenciaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.AlunoFrequencia;
import br.com.eduacompanha.model.AnoLetivo;
import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.RegistroFrequencia;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.model.enums.StatusRegistro;
import br.com.eduacompanha.model.enums.TipoOcorrencia;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.util.SessaoUtil;

// Frequência (RN-43 a 56). Regras copiadas do frequencia.service.js do site.
// Lançar falta, atraso, saída; registrar volta; cancelar; "chegou atrasado"; e o cálculo do percentual.
public class FrequenciaService {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MOTIVO_CHEGOU_ATRASADO = "Aluno chegou atrasado depois da conferência em sala.";

    // ------------------------------------------------------------------
    // Records: pacotes de dados prontos para o menu mostrar
    // ------------------------------------------------------------------

    // Uma ocorrência do dia, com o nome do aluno e quantas aulas ele perdeu por ela.
    public record OcorrenciaDoDia(RegistroFrequencia registro, String nomeAluno, int aulasPerdidas) {
        @Override
        public String toString() {
            return nomeAluno + " - " + FrequenciaService.descrever(registro) + " (" + aulasPerdidas + " aula(s))";
        }
    }

    // Uma linha da chamada: o aluno, a frequência dele e o que aconteceu no dia.
    public record LinhaChamada(Matricula matricula, Aluno aluno, AlunoFrequencia frequencia,
            List<OcorrenciaDoDia> ocorrencias) {
        @Override
        public String toString() {
            return aluno.getNomeAluno();
        }
    }

    // Tudo o que a tela da chamada mostra para uma turma numa data.
    public record DadosChamada(Turma turma, LocalDate data, List<HorarioAula> aulasDoDia, List<LinhaChamada> linhas) {
        // todas as ocorrências do dia, de todos os alunos, numa lista só
        public List<OcorrenciaDoDia> ocorrencias() {
            List<OcorrenciaDoDia> todas = new ArrayList<>();
            for (LinhaChamada l : linhas) {
                todas.addAll(l.ocorrencias());
            }
            return todas;
        }
    }

    private IRegistroFrequenciaDao registroDao;
    private IMatriculaDao matriculaDao;
    private ITurmaDao turmaDao;
    private IAlunoDao alunoDao;
    private IAnoLetivoDao anoLetivoDao;
    private TurnoService turnoService;
    private Clock relogio;

    public FrequenciaService(IRegistroFrequenciaDao registroDao, IMatriculaDao matriculaDao, ITurmaDao turmaDao,
            IAlunoDao alunoDao, IAnoLetivoDao anoLetivoDao, TurnoService turnoService, Clock relogio) {
        this.registroDao = registroDao;
        this.matriculaDao = matriculaDao;
        this.turmaDao = turmaDao;
        this.alunoDao = alunoDao;
        this.anoLetivoDao = anoLetivoDao;
        this.turnoService = turnoService;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------
    // Consultas para a tela da chamada
    // ------------------------------------------------------------------

    // Turmas do ano atual da escola da secretaria.
    public List<Turma> turmasParaChamada() {
        Usuario usuario = exigirSecretaria();
        return turmaDao.consultarPorEscola(usuario.getCie(), LocalDate.now(relogio).getYear());
    }

    // Lista da chamada: cada aluno matriculado na data, com a frequência e as ocorrências do dia.
    public DadosChamada dadosDaChamada(int idTurma, LocalDate data) {
        exigirSecretaria();
        Turma turma = buscarTurma(idTurma);
        List<HorarioAula> versoes = turnoService.versoes(turma.getCie(), turma.getTurno());
        List<HorarioAula> aulas = turnoService.aulasDoDia(versoes, data);

        List<LinhaChamada> linhas = new ArrayList<>();
        for (Matricula m : matriculaDao.consultarAtivasDaTurma(idTurma)) {
            if (m.getDataInicio().isAfter(data) || (m.getDataFim() != null && m.getDataFim().isBefore(data))) {
                continue; // não estava matriculado nesta data
            }
            Aluno aluno = alunoDao.consultar(m.getIdAluno());
            List<RegistroFrequencia> registros = registroDao.consultarPorMatricula(m.getIdMatricula());

            AlunoFrequencia freq = calcular(m, versoes, registros, anoLetivoDao.consultar(turma.getAnoLetivo()));
            freq.setAluno(aluno);
            freq.setTurma(turma);

            List<OcorrenciaDoDia> doDia = new ArrayList<>();
            for (RegistroFrequencia r : registros) {
                if (r.getStatus() == StatusRegistro.ATIVO && r.getData().equals(data)) {
                    doDia.add(new OcorrenciaDoDia(r, aluno.getNomeAluno(), contarPerdidas(versoes, r)));
                }
            }
            linhas.add(new LinhaChamada(m, aluno, freq, doDia));
        }
        linhas.sort((a, b) -> a.aluno().getNomeAluno().compareToIgnoreCase(b.aluno().getNomeAluno()));
        return new DadosChamada(turma, data, aulas, linhas);
    }

    // Frequência de uma matrícula, já com aluno e turma (o AlertaService usa).
    public AlunoFrequencia frequenciaDaMatricula(Matricula m) {
        Turma turma = turmaDao.consultar(m.getIdTurma());
        SessaoUtil.exigirAcessoEscola(turma.getCie());
        List<HorarioAula> versoes = turnoService.versoes(turma.getCie(), turma.getTurno());
        AlunoFrequencia freq = calcular(m, versoes, registroDao.consultarPorMatricula(m.getIdMatricula()),
                anoLetivoDao.consultar(turma.getAnoLetivo()));
        freq.setAluno(alunoDao.consultar(m.getIdAluno()));
        freq.setTurma(turma);
        return freq;
    }

    // ------------------------------------------------------------------
    // RN-43 a 51: lançar uma ocorrência
    // ------------------------------------------------------------------
    public RegistroFrequencia lancar(int idMatricula, LocalDate data, TipoOcorrencia tipo, LocalTime entrada,
            LocalTime saida, String obs) {
        Usuario usuario = exigirSecretaria();
        if (tipo == null) {
            throw new RegraNegocioException("Tipo de registro inválido.");
        }
        Matricula matricula = matriculaDao.consultar(idMatricula);
        if (matricula == null) {
            throw new NaoEncontradoException("Matrícula não encontrada.");
        }
        Turma turma = buscarTurma(matricula.getIdTurma());
        AnoLetivo ano = anoLetivoDao.consultar(turma.getAnoLetivo());
        LocalDate hoje = LocalDate.now(relogio);

        // RN-44: turma, data e matrícula
        if (turma.getAnoLetivo() != hoje.getYear()) {
            throw new RegraNegocioException(
                    "Esta turma é de um ano letivo encerrado. Não é possível lançar frequência.");
        }
        if (data == null || data.isAfter(hoje)) {
            throw new RegraNegocioException("Escolha uma data até hoje.");
        }
        if (data.isBefore(ano.getDataInicio()) || data.isAfter(ano.getDataFim())) {
            throw new RegraNegocioException("A data precisa estar dentro do ano letivo ("
                    + ano.getDataInicio().format(BR) + " a " + ano.getDataFim().format(BR) + ").");
        }
        if (data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new RegraNegocioException("Não há aula aos sábados e domingos.");
        }
        if (matricula.getDataInicio().isAfter(data)
                || (matricula.getDataFim() != null && matricula.getDataFim().isBefore(data))) {
            throw new RegraNegocioException("O aluno não estava matriculado nesta turma nesta data.");
        }

        // RN-45: precisa ter grade e aula no dia
        List<HorarioAula> versoes = turnoService.versoes(turma.getCie(), turma.getTurno());
        List<HorarioAula> aulas = turnoService.aulasDoDia(versoes, data);
        if (aulas == null) {
            throw new RegraNegocioException("A escola ainda não montou a grade de horários do "
                    + nomeDoTurno(turma.getTurno()) + ". Configure em \"Turnos e envio\".");
        }
        if (aulas.isEmpty()) {
            throw new RegraNegocioException(
                    "O " + nomeDoTurno(turma.getTurno()) + " não tem aula na " + nomeDoDia(data) + ".");
        }

        // RN-46: as horas que cada tipo pede
        if (tipo == TipoOcorrencia.FALTA) {
            entrada = null;
            saida = null;
        } else {
            validarHoras(tipo, entrada, saida, aulas);
            if (tipo == TipoOcorrencia.ATRASO) {
                saida = null;
            }
            if (tipo == TipoOcorrencia.SAIDA_ANTECIPADA) {
                entrada = null;
            }
        }

        // RN-51: observação opcional, no máximo 200 caracteres
        String texto = obs == null ? "" : obs.trim();
        if (texto.length() > 200) {
            texto = texto.substring(0, 200);
        }

        // RN-47 a 49: combinação com o que já tem no dia
        List<RegistroFrequencia> doDia = ativosDoDia(idMatricula, data);
        if (tipo == TipoOcorrencia.FALTA && !doDia.isEmpty()) {
            throw new RegraNegocioException("O aluno tem outra ocorrência neste dia, então esteve na escola. "
                    + "Cancele essa ocorrência antes de lançar falta.");
        }
        for (RegistroFrequencia r : doDia) {
            if (tipo != TipoOcorrencia.FALTA && r.getTipoOcorrencia() == TipoOcorrencia.FALTA) {
                throw new RegraNegocioException(
                        "O aluno está com falta neste dia. Use \"Chegou atrasado\" ou cancele a falta antes.");
            }
            if (tipo == TipoOcorrencia.ATRASO && r.getTipoOcorrencia() == TipoOcorrencia.ATRASO) {
                throw new RegraNegocioException("Este aluno já tem atraso registrado neste dia.");
            }
            if (tipo == TipoOcorrencia.SAIDA_ANTECIPADA && r.getTipoOcorrencia() == TipoOcorrencia.SAIDA_ANTECIPADA) {
                throw new RegraNegocioException("Este aluno já tem uma saída sem volta neste dia. "
                        + "Registre a volta dela antes de lançar outra saída.");
            }
        }

        RegistroFrequencia novo = new RegistroFrequencia();
        novo.setIdMatricula(idMatricula);
        novo.setData(data);
        novo.setTipoOcorrencia(tipo);
        novo.setHoraEntrada(entrada);
        novo.setHoraSaida(saida);

        for (RegistroFrequencia r : doDia) {
            if (sobrepoe(novo, r)) {
                throw new RegraNegocioException("Este horário se sobrepõe a outra ocorrência do aluno neste dia.");
            }
        }

        // RN-50: se perde todas as aulas, é falta
        int perdidas = contarPerdidas(versoes, novo);
        if (tipo != TipoOcorrencia.FALTA && perdidas == aulas.size()) {
            throw new RegraNegocioException("Com esse horário o aluno perde todas as " + aulas.size()
                    + " aulas do dia. Nesse caso, registre falta.");
        }

        // RN-51: grava quem registrou e quando
        novo.setObservacao(texto.isEmpty() ? null : texto);
        novo.setStatus(StatusRegistro.ATIVO);
        novo.setRegistradoEm(LocalDateTime.now(relogio));
        novo.setIdUsuarioRegistro(usuario.getIdUsuario());
        return registroDao.cadastrar(novo);
    }

    // ------------------------------------------------------------------
    // RN-52: registrar a volta de uma saída
    // ------------------------------------------------------------------
    public RegistroFrequencia registrarVolta(int idRegistroSaida, LocalTime volta) {
        exigirSecretaria();
        RegistroFrequencia saida = buscarRegistro(idRegistroSaida);
        if (saida.getTipoOcorrencia() != TipoOcorrencia.SAIDA_ANTECIPADA
                || saida.getStatus() != StatusRegistro.ATIVO) {
            throw new RegraNegocioException("Só é possível registrar a volta de uma saída sem volta.");
        }
        Turma turma = turmaDaMatricula(saida.getIdMatricula());
        List<HorarioAula> versoes = turnoService.versoes(turma.getCie(), turma.getTurno());
        List<HorarioAula> aulas = turnoService.aulasDoDia(versoes, saida.getData());
        if (aulas == null) {
            aulas = new ArrayList<>();
        }
        validarHoras(TipoOcorrencia.SAIDA_COM_RETORNO, volta, saida.getHoraSaida(), aulas);

        // vira "saiu e voltou" no mesmo registro
        saida.setTipoOcorrencia(TipoOcorrencia.SAIDA_COM_RETORNO);
        saida.setHoraEntrada(volta);
        for (RegistroFrequencia outro : ativosDoDia(saida.getIdMatricula(), saida.getData())) {
            if (outro.getIdRegistroFreq() != saida.getIdRegistroFreq() && sobrepoe(saida, outro)) {
                throw new ValidacaoException("horaEntrada",
                        "Este horário se sobrepõe a outra ocorrência do aluno neste dia.");
            }
        }
        registroDao.alterar(saida, saida.getIdRegistroFreq());
        return saida;
    }

    // ------------------------------------------------------------------
    // RN-53: cancelar (nunca apaga)
    // ------------------------------------------------------------------
    public RegistroFrequencia cancelar(int idRegistro, String motivo) {
        Usuario usuario = exigirSecretaria();
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.length() < 5) {
            throw new ValidacaoException("motivo", "Explique o motivo em pelo menos 5 caracteres.");
        }
        RegistroFrequencia registro = buscarRegistro(idRegistro);
        if (registro.getStatus() != StatusRegistro.ATIVO) {
            throw new RegraNegocioException("Este registro já foi cancelado.");
        }
        registroDao.cancelar(idRegistro, texto, usuario.getIdUsuario(), LocalDateTime.now(relogio));

        if (registro.getTipoOcorrencia() == TipoOcorrencia.FALTA) {
            // TODO parte F (RN-62): notificacaoService.interromperSeSemFalta(idAluno, registro.getData());
        }
        return registroDao.consultar(idRegistro);
    }

    // ------------------------------------------------------------------
    // RN-54: a falta de hoje vira atraso
    // ------------------------------------------------------------------
    public RegistroFrequencia chegouAtrasado(int idRegistroFalta, LocalTime entrada) {
        Usuario usuario = exigirSecretaria();
        RegistroFrequencia falta = buscarRegistro(idRegistroFalta);
        if (falta.getTipoOcorrencia() != TipoOcorrencia.FALTA || falta.getStatus() != StatusRegistro.ATIVO) {
            throw new RegraNegocioException("Só é possível trocar uma falta registrada.");
        }
        if (!falta.getData().equals(LocalDate.now(relogio))) {
            throw new RegraNegocioException("\"Chegou atrasado\" vale para faltas de hoje. "
                    + "Para outras datas, cancele a falta e registre o atraso.");
        }
        Turma turma = turmaDaMatricula(falta.getIdMatricula());
        List<HorarioAula> versoes = turnoService.versoes(turma.getCie(), turma.getTurno());
        List<HorarioAula> aulas = turnoService.aulasDoDia(versoes, falta.getData());
        if (aulas == null) {
            aulas = new ArrayList<>();
        }
        validarHoras(TipoOcorrencia.ATRASO, entrada, null, aulas);

        RegistroFrequencia atraso = new RegistroFrequencia();
        atraso.setIdMatricula(falta.getIdMatricula());
        atraso.setData(falta.getData());
        atraso.setTipoOcorrencia(TipoOcorrencia.ATRASO);
        atraso.setHoraEntrada(entrada);
        if (contarPerdidas(versoes, atraso) == aulas.size()) {
            throw new ValidacaoException("horaEntrada",
                    "Com essa hora o aluno perdeu todas as aulas do dia: mantenha a falta.");
        }

        LocalDateTime agora = LocalDateTime.now(relogio);
        registroDao.cancelar(idRegistroFalta, MOTIVO_CHEGOU_ATRASADO, usuario.getIdUsuario(), agora);
        atraso.setStatus(StatusRegistro.ATIVO);
        atraso.setRegistradoEm(agora);
        atraso.setIdUsuarioRegistro(usuario.getIdUsuario());
        RegistroFrequencia criado = registroDao.cadastrar(atraso);

        // TODO parte F (RN-62): notificacaoService.interromperSeSemFalta(idAluno, falta.getData());
        return criado;
    }

    // ------------------------------------------------------------------
    // RN-55 e 56: o cálculo. Não olha a Sessao nem o banco: só faz a conta.
    // ------------------------------------------------------------------
    public AlunoFrequencia calcular(Matricula m, List<HorarioAula> versoes, List<RegistroFrequencia> registros,
            AnoLetivo ano) {
        // só os registros ativos contam
        List<RegistroFrequencia> ativos = new ArrayList<>();
        int diasComFalta = 0;
        for (RegistroFrequencia r : registros) {
            if (r.getStatus() == StatusRegistro.ATIVO) {
                ativos.add(r);
                if (r.getTipoOcorrencia() == TipoOcorrencia.FALTA) {
                    diasComFalta++; // RN-56: não limita ao período
                }
            }
        }

        AlunoFrequencia resultado = new AlunoFrequencia();
        resultado.setDiasComFalta(diasComFalta);
        if (versoes == null || versoes.isEmpty()) {
            resultado.setPercentual(null); // turno sem grade: o menu mostra "-"
            return resultado;
        }

        // período: do maior início até o menor fim
        LocalDate inicio = maior(m.getDataInicio(), ano.getDataInicio());
        LocalDate fim = LocalDate.now(relogio);
        if (m.getDataFim() != null) {
            fim = menor(fim, m.getDataFim());
        }
        fim = menor(fim, ano.getDataFim());

        // aulas previstas: soma das aulas de cada dia do período
        int previstas = 0;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            List<HorarioAula> aulas = turnoService.aulasDoDia(versoes, d);
            if (aulas != null) {
                previstas += aulas.size();
            }
        }

        // aulas perdidas: por dia, junta as ordens num Set (uma aula só conta uma vez no dia)
        Map<LocalDate, Set<Integer>> perdidasPorDia = new LinkedHashMap<>();
        for (RegistroFrequencia r : ativos) {
            if (r.getData().isBefore(inicio) || r.getData().isAfter(fim)) {
                continue;
            }
            List<HorarioAula> perdidas = turnoService.aulasPerdidas(versoes, r);
            if (perdidas == null) {
                continue;
            }
            Set<Integer> ordens = perdidasPorDia.computeIfAbsent(r.getData(), d -> new HashSet<>());
            for (HorarioAula h : perdidas) {
                ordens.add(h.getOrdemAula());
            }
        }
        int aulasComFalta = 0;
        for (Set<Integer> ordens : perdidasPorDia.values()) {
            aulasComFalta += ordens.size();
        }

        resultado.setAulasPrevistas(previstas);
        resultado.setAulasComFalta(aulasComFalta);
        if (previstas == 0) {
            resultado.setPercentual(null);
        } else {
            resultado.setPercentual(Math.max(0, (previstas - aulasComFalta) * 100.0 / previstas));
        }
        return resultado;
    }

    // Texto curto de uma ocorrência: "Atraso, chegou 09:30". O menu usa.
    public static String descrever(RegistroFrequencia r) {
        switch (r.getTipoOcorrencia()) {
            case FALTA:
                return "Falta";
            case ATRASO:
                return "Atraso, chegou " + r.getHoraEntrada();
            case SAIDA_ANTECIPADA:
                return "Saída antecipada, saiu " + r.getHoraSaida();
            case SAIDA_COM_RETORNO:
                return "Saiu " + r.getHoraSaida() + " e voltou " + r.getHoraEntrada();
            default:
                return "";
        }
    }

    // ------------------------------------------------------------------
    // Métodos de apoio (private)
    // ------------------------------------------------------------------

    // RN-43: só a secretaria lança
    private Usuario exigirSecretaria() {
        Usuario usuario = SessaoUtil.getUsuario();
        if (usuario == null || usuario.getPerfil() != Perfil.SECRETARIA) {
            throw new AcessoNegadoException("Seu perfil não tem permissão para esta ação.");
        }
        return usuario;
    }

    // Busca a turma e confere se é da escola de quem está logado (RN-37)
    private Turma buscarTurma(int idTurma) {
        Turma turma = turmaDao.consultar(idTurma);
        if (turma == null) {
            throw new NaoEncontradoException("Turma não encontrada.");
        }
        SessaoUtil.exigirAcessoEscola(turma.getCie());
        return turma;
    }

    private Turma turmaDaMatricula(int idMatricula) {
        Matricula m = matriculaDao.consultar(idMatricula);
        if (m == null) {
            throw new NaoEncontradoException("Matrícula não encontrada.");
        }
        return buscarTurma(m.getIdTurma());
    }

    // Busca o registro e confere a escola
    private RegistroFrequencia buscarRegistro(int idRegistro) {
        RegistroFrequencia r = registroDao.consultar(idRegistro);
        if (r == null) {
            throw new NaoEncontradoException("Registro não encontrado.");
        }
        turmaDaMatricula(r.getIdMatricula()); // só para conferir o acesso à escola
        return r;
    }

    // Registros ATIVOS da matrícula naquele dia
    private List<RegistroFrequencia> ativosDoDia(int idMatricula, LocalDate data) {
        List<RegistroFrequencia> ativos = new ArrayList<>();
        for (RegistroFrequencia r : registroDao.consultarDoDia(idMatricula, data)) {
            if (r.getStatus() == StatusRegistro.ATIVO) {
                ativos.add(r);
            }
        }
        return ativos;
    }

    // RN-46: confere as horas de cada tipo. Junta os erros e lança uma ValidacaoException.
    private void validarHoras(TipoOcorrencia tipo, LocalTime entrada, LocalTime saida, List<HorarioAula> aulas) {
        boolean precisaEntrada = tipo == TipoOcorrencia.ATRASO || tipo == TipoOcorrencia.SAIDA_COM_RETORNO;
        boolean precisaSaida = tipo == TipoOcorrencia.SAIDA_ANTECIPADA || tipo == TipoOcorrencia.SAIDA_COM_RETORNO;

        Map<String, String> erros = new LinkedHashMap<>();
        if (precisaEntrada && entrada == null) {
            erros.put("horaEntrada", tipo == TipoOcorrencia.ATRASO ? "Informe a hora em que o aluno chegou."
                    : "Informe a hora em que o aluno voltou.");
        }
        if (precisaSaida && saida == null) {
            erros.put("horaSaida", "Informe a hora em que o aluno saiu.");
        }

        if (erros.isEmpty() && !aulas.isEmpty()) {
            // período de aulas do dia: do início da 1ª ao fim da última
            LocalTime inicio = aulas.get(0).getHoraInicio();
            LocalTime fim = aulas.get(aulas.size() - 1).getHoraFim();
            String foraDoPeriodo = "Use um horário entre " + inicio + " e " + fim + ", o período de aulas do dia.";
            if (precisaEntrada && (!entrada.isAfter(inicio) || !entrada.isBefore(fim))) {
                erros.put("horaEntrada", foraDoPeriodo);
            }
            if (precisaSaida && (!saida.isAfter(inicio) || !saida.isBefore(fim))) {
                erros.put("horaSaida", foraDoPeriodo);
            }
            if (tipo == TipoOcorrencia.SAIDA_COM_RETORNO && !erros.containsKey("horaEntrada")
                    && !entrada.isAfter(saida)) {
                erros.put("horaEntrada", "A volta precisa ser depois da saída.");
            }
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    // RN-49: cada ocorrência ocupa um intervalo do dia; dois intervalos não podem se cruzar.
    // atraso: 00:00 até a entrada. saída: da saída até 23:59. saiu e voltou: da saída até a volta.
    private boolean sobrepoe(RegistroFrequencia a, RegistroFrequencia b) {
        return inicioDoIntervalo(a).isBefore(fimDoIntervalo(b)) && inicioDoIntervalo(b).isBefore(fimDoIntervalo(a));
    }

    private LocalTime inicioDoIntervalo(RegistroFrequencia r) {
        return r.getTipoOcorrencia() == TipoOcorrencia.ATRASO ? LocalTime.of(0, 0) : r.getHoraSaida();
    }

    private LocalTime fimDoIntervalo(RegistroFrequencia r) {
        return r.getTipoOcorrencia() == TipoOcorrencia.SAIDA_ANTECIPADA ? LocalTime.of(23, 59) : r.getHoraEntrada();
    }

    private int contarPerdidas(List<HorarioAula> versoes, RegistroFrequencia r) {
        List<HorarioAula> perdidas = turnoService.aulasPerdidas(versoes, r);
        return perdidas == null ? 0 : perdidas.size();
    }

    private static String nomeDoTurno(Turno turno) {
        switch (turno) {
            case MANHA:
                return "turno da manhã";
            case TARDE:
                return "turno da tarde";
            case NOITE:
                return "turno da noite";
            default:
                return "turno integral";
        }
    }

    private static String nomeDoDia(LocalDate data) {
        String[] nomes = { "", "segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo" };
        return nomes[data.getDayOfWeek().getValue()];
    }

    private static LocalDate maior(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate menor(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}

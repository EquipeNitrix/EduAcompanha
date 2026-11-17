package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.eduacompanha.dao.IAcompanhamentoCasoDao;
import br.com.eduacompanha.dao.IAlunoDao;
import br.com.eduacompanha.dao.ICasoBuscaAtivaDao;
import br.com.eduacompanha.dao.IMatriculaDao;
import br.com.eduacompanha.dao.IResponsavelDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.AcompanhamentoCaso;
import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.CasoBuscaAtiva;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Responsavel;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.model.enums.StatusCaso;
import br.com.eduacompanha.model.enums.TipoRegistro;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.util.TextoUtil;

// Busca Ativa (RN-65 a 69). Regras copiadas do busca-ativa.service.js do site.
// Só a secretaria usa, e só na própria escola.
public class BuscaAtivaService {

    // Listas fixas do catálogo. O menu mostra estas listas para a pessoa escolher.
    public static final List<String> CANAIS = List.of("Ligação telefônica", "WhatsApp", "Visita domiciliar",
            "Reunião na escola", "Bilhete ou carta", "Outro");
    public static final List<String> RESULTADOS = List.of("Contato efetivo", "Sem sucesso", "Número inválido",
            "Retorno agendado");
    public static final List<String> TIPOS_ACAO = List.of("Reunião com a família", "Encaminhamento",
            "Acordo de retorno", "Ação pedagógica", "Outro");
    public static final List<String> DESFECHOS = List.of("Aluno retornou à frequência regular",
            "Aluno transferido de escola", "Encaminhado à rede de proteção", "Aluno não localizado", "Outro");

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Uma linha da lista de casos: o caso + o nome do aluno.
    public record ResumoCaso(CasoBuscaAtiva caso, Aluno aluno, int tentativas) {
        @Override
        public String toString() {
            return "#" + caso.getIdCaso() + " - " + aluno.getNomeAluno() + " (aberto em "
                    + caso.getDataAbertura().format(BR) + ")";
        }
    }

    // Tudo o que a tela "Ver caso" mostra.
    public record DetalheCaso(CasoBuscaAtiva caso, Aluno aluno, List<Responsavel> responsaveis,
            List<AcompanhamentoCaso> registros) {
    }

    private ICasoBuscaAtivaDao casoDao;
    private IAcompanhamentoCasoDao acompanhamentoDao;
    private IAlunoDao alunoDao;
    private IMatriculaDao matriculaDao;
    private ITurmaDao turmaDao;
    private IResponsavelDao responsavelDao;
    private Clock relogio;

    public BuscaAtivaService(ICasoBuscaAtivaDao casoDao, IAcompanhamentoCasoDao acompanhamentoDao,
            IAlunoDao alunoDao, IMatriculaDao matriculaDao, ITurmaDao turmaDao, IResponsavelDao responsavelDao,
            Clock relogio) {
        this.casoDao = casoDao;
        this.acompanhamentoDao = acompanhamentoDao;
        this.alunoDao = alunoDao;
        this.matriculaDao = matriculaDao;
        this.turmaDao = turmaDao;
        this.responsavelDao = responsavelDao;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------
    // Consultas (o que os menus mostram)
    // ------------------------------------------------------------------

    // Casos da escola de quem está logado. status null = todos.
    public List<ResumoCaso> listar(StatusCaso status) {
        Usuario usuario = exigirSecretaria();
        List<ResumoCaso> lista = new ArrayList<>();
        for (CasoBuscaAtiva caso : casoDao.consultarPorEscola(usuario.getCie(), status)) {
            Aluno aluno = alunoDao.consultar(caso.getIdAluno());
            int tentativas = 0;
            for (AcompanhamentoCaso a : acompanhamentoDao.consultarPorCaso(caso.getIdCaso())) {
                if (a.getTipoRegistro() == TipoRegistro.TENTATIVA) {
                    tentativas++;
                }
            }
            lista.add(new ResumoCaso(caso, aluno, tentativas));
        }
        return lista;
    }

    public DetalheCaso detalhe(int idCaso) {
        exigirSecretaria();
        CasoBuscaAtiva caso = buscarCaso(idCaso);
        Aluno aluno = alunoDao.consultar(caso.getIdAluno());
        List<Responsavel> responsaveis = responsavelDao.consultarPorAluno(caso.getIdAluno());
        List<AcompanhamentoCaso> registros = acompanhamentoDao.consultarPorCaso(idCaso);
        return new DetalheCaso(caso, aluno, responsaveis, registros);
    }

    // Alunos que podem ganhar um caso: matrícula ativa na escola e sem caso aberto.
    public List<Aluno> candidatos() {
        Usuario usuario = exigirSecretaria();
        int anoAtual = LocalDate.now(relogio).getYear();
        List<Aluno> alunos = new ArrayList<>();
        for (Turma turma : turmaDao.consultarPorEscola(usuario.getCie(), anoAtual)) {
            for (Matricula m : matriculaDao.consultarAtivasDaTurma(turma.getIdTurma())) {
                if (casoDao.consultarAbertoDoAluno(m.getIdAluno()) == null) {
                    alunos.add(alunoDao.consultar(m.getIdAluno()));
                }
            }
        }
        return alunos;
    }

    public List<Responsavel> responsaveisDoCaso(int idCaso) {
        exigirSecretaria();
        CasoBuscaAtiva caso = buscarCaso(idCaso);
        return responsavelDao.consultarPorAluno(caso.getIdAluno());
    }

    // ------------------------------------------------------------------
    // RN-65: abrir caso
    // ------------------------------------------------------------------
    public CasoBuscaAtiva abrirCaso(int idAluno, LocalDate abertura, String motivo) {
        Usuario usuario = exigirSecretaria();
        LocalDate hoje = LocalDate.now(relogio);

        Map<String, String> erros = new LinkedHashMap<>();
        if (idAluno == 0) {
            erros.put("alunoId", "Escolha o aluno.");
        }
        if (abertura == null) {
            erros.put("abertura", "Informe a data de abertura.");
        } else if (abertura.isAfter(hoje)) {
            erros.put("abertura", "A data não pode ser futura.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        // O aluno precisa ter matrícula ativa, no ano atual, numa turma desta escola.
        Matricula matricula = matriculaDao.consultarAtivaDoAluno(idAluno, hoje.getYear());
        Turma turma = null;
        if (matricula != null) {
            turma = turmaDao.consultar(matricula.getIdTurma());
        }
        if (turma == null || !turma.getCie().equals(usuario.getCie())) {
            throw new RegraNegocioException("O aluno precisa ter matrícula ativa nesta escola.");
        }

        // Um caso aberto por aluno.
        if (casoDao.consultarAbertoDoAluno(idAluno) != null) {
            throw new ValidacaoException("alunoId", "Este aluno já tem um caso aberto.");
        }

        CasoBuscaAtiva caso = new CasoBuscaAtiva();
        caso.setIdAluno(idAluno);
        caso.setCie(usuario.getCie());
        caso.setDataAbertura(abertura);
        caso.setMotivo(TextoUtil.limparEspacos(motivo)); // opcional: vazio vira null
        caso.setStatus(StatusCaso.ABERTO);
        caso.setIdUsuarioAbertura(usuario.getIdUsuario());
        return casoDao.cadastrar(caso);
    }

    // ------------------------------------------------------------------
    // RN-67: registrar tentativa de contato
    // ------------------------------------------------------------------
    public AcompanhamentoCaso registrarTentativa(int idCaso, String canal, LocalDateTime dataHora,
            String descricao, String resultado, Integer idResponsavel) {
        Usuario usuario = exigirSecretaria();
        CasoBuscaAtiva caso = casoAbertoParaEditar(idCaso);
        String texto = TextoUtil.limparEspacos(descricao);

        Map<String, String> erros = new LinkedHashMap<>();
        if (canal == null || !CANAIS.contains(canal)) {
            erros.put("canal", "Escolha o canal.");
        }
        String erroData = validarDataHora(dataHora, caso, "A tentativa");
        if (erroData != null) {
            erros.put("dataHora", erroData);
        }
        if (texto == null || texto.length() < 5) {
            erros.put("descricao", "Descreva a conversa em pelo menos 5 caracteres.");
        }
        if (resultado == null || !RESULTADOS.contains(resultado)) {
            erros.put("resultado", "Escolha o resultado.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        // O responsável é opcional, mas se vier precisa ser deste aluno.
        if (idResponsavel != null) {
            Responsavel r = responsavelDao.consultar(idResponsavel);
            if (r == null || r.getIdAluno() != caso.getIdAluno()) {
                throw new ValidacaoException("responsavelId", "Escolha um responsável deste aluno.");
            }
        }

        AcompanhamentoCaso a = new AcompanhamentoCaso();
        a.setIdCaso(idCaso);
        a.setTipoRegistro(TipoRegistro.TENTATIVA);
        a.setDataHora(dataHora);
        a.setDescricao(texto);
        a.setCanal(canal);
        a.setResultado(resultado);
        a.setTipoAcao(null); // tentativa não tem tipo de ação (CHECK do banco)
        a.setIdResponsavel(idResponsavel);
        a.setIdUsuario(usuario.getIdUsuario());
        return acompanhamentoDao.cadastrar(a);
    }

    // ------------------------------------------------------------------
    // RN-68: registrar ação
    // ------------------------------------------------------------------
    public AcompanhamentoCaso registrarAcao(int idCaso, String tipoAcao, LocalDateTime dataHora, String descricao) {
        Usuario usuario = exigirSecretaria();
        CasoBuscaAtiva caso = casoAbertoParaEditar(idCaso);
        String texto = TextoUtil.limparEspacos(descricao);

        Map<String, String> erros = new LinkedHashMap<>();
        if (tipoAcao == null || !TIPOS_ACAO.contains(tipoAcao)) {
            erros.put("tipoAcao", "Escolha o tipo.");
        }
        String erroData = validarDataHora(dataHora, caso, "A ação");
        if (erroData != null) {
            erros.put("dataHora", erroData);
        }
        if (texto == null || texto.length() < 5) {
            erros.put("descricao", "Descreva a ação em pelo menos 5 caracteres.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        AcompanhamentoCaso a = new AcompanhamentoCaso();
        a.setIdCaso(idCaso);
        a.setTipoRegistro(TipoRegistro.ACAO);
        a.setDataHora(dataHora);
        a.setDescricao(texto);
        a.setCanal(null); // ação não tem canal nem resultado (CHECK do banco)
        a.setResultado(null);
        a.setTipoAcao(tipoAcao);
        a.setIdResponsavel(null);
        a.setIdUsuario(usuario.getIdUsuario());
        return acompanhamentoDao.cadastrar(a);
    }

    // ------------------------------------------------------------------
    // RN-69: encerrar caso
    // ------------------------------------------------------------------
    public CasoBuscaAtiva encerrar(int idCaso, LocalDate data, String desfecho, String observacao) {
        exigirSecretaria();
        CasoBuscaAtiva caso = casoAbertoParaEditar(idCaso);
        LocalDate hoje = LocalDate.now(relogio);

        Map<String, String> erros = new LinkedHashMap<>();
        if (data == null) {
            erros.put("data", "Informe a data.");
        } else if (data.isBefore(caso.getDataAbertura())) {
            erros.put("data", "A data não pode ser anterior à abertura.");
        } else if (data.isAfter(hoje)) {
            erros.put("data", "A data não pode ser futura.");
        }
        if (desfecho == null || !DESFECHOS.contains(desfecho)) {
            erros.put("desfecho", "Escolha o desfecho.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        caso.setStatus(StatusCaso.ENCERRADO);
        caso.setDataEncerramento(data);
        caso.setDesfecho(desfecho);
        caso.setObservacaoFinal(TextoUtil.limparEspacos(observacao));
        casoDao.alterar(caso, idCaso);
        return caso;
    }

    // ------------------------------------------------------------------
    // Métodos de apoio (private: só este Service usa)
    // ------------------------------------------------------------------

    // RN-65: só a secretaria. Devolve quem está logado para não buscar de novo.
    private Usuario exigirSecretaria() {
        Usuario usuario = SessaoUtil.getUsuario();
        if (usuario == null || usuario.getPerfil() != Perfil.SECRETARIA) {
            throw new AcessoNegadoException("Seu perfil não tem permissão para esta ação.");
        }
        return usuario;
    }

    // Busca o caso e confere se é da escola de quem está logado (RN-37).
    private CasoBuscaAtiva buscarCaso(int idCaso) {
        CasoBuscaAtiva caso = casoDao.consultar(idCaso);
        if (caso == null) {
            throw new NaoEncontradoException("Caso não encontrado.");
        }
        SessaoUtil.exigirAcessoEscola(caso.getCie());
        return caso;
    }

    // RN-66: caso encerrado só aceita consulta.
    private CasoBuscaAtiva casoAbertoParaEditar(int idCaso) {
        CasoBuscaAtiva caso = buscarCaso(idCaso);
        if (caso.getStatus() != StatusCaso.ABERTO) {
            throw new RegraNegocioException("Este caso está encerrado e aceita apenas consulta.");
        }
        return caso;
    }

    // RN-67 e 68: data e hora entre a abertura do caso e agora. Devolve a mensagem
    // de erro, ou null.
    private String validarDataHora(LocalDateTime dataHora, CasoBuscaAtiva caso, String oQue) {
        if (dataHora == null) {
            return "Informe data e hora.";
        }
        if (dataHora.toLocalDate().isBefore(caso.getDataAbertura())) {
            return oQue + " não pode ser anterior à abertura do caso (" + caso.getDataAbertura().format(BR) + ").";
        }
        if (dataHora.isAfter(LocalDateTime.now(relogio))) {
            return "A data e hora não podem ser futuras.";
        }
        return null;
    }
}

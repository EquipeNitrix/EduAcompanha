package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.dao.IAlunoDao;
import br.com.eduacompanha.dao.IContatoResponsavelDao;
import br.com.eduacompanha.dao.IEnvioNotificacaoDao;
import br.com.eduacompanha.dao.IEscolaDao;
import br.com.eduacompanha.dao.IMatriculaDao;
import br.com.eduacompanha.dao.INotificacaoDao;
import br.com.eduacompanha.dao.IRegistroFrequenciaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.ContatoResponsavel;
import br.com.eduacompanha.model.EnvioNotificacao;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Notificacao;
import br.com.eduacompanha.model.RegistroFrequencia;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.model.enums.StatusNotificacao;
import br.com.eduacompanha.model.enums.StatusRegistro;
import br.com.eduacompanha.model.enums.TipoOcorrencia;
import br.com.eduacompanha.util.SessaoUtil;

// Notificação automática aos responsáveis (RN-60 a 64).
// Regras copiadas do notificacao.service.js e do fila.js do site.
// O "envio" é simulado: grava uma linha em ENVIO_NOTIFICACAO.
public class NotificacaoService {

    public static final int PRAZO_RESPOSTA_HORAS = 24;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // O que o processarFila fez: quantas notificações novas e quantas andaram na fila.
    public record ResultadoFila(int enviadas, int avancadas) {
    }

    // Uma linha da lista de notificações.
    public record ResumoNotificacao(Notificacao notificacao, Aluno aluno, Turma turma) {
        @Override
        public String toString() {
            return notificacao.getDataFalta().format(BR) + " - " + aluno.getNomeAluno() + " ("
                    + notificacao.getStatus() + ")";
        }
    }

    // Tudo o que a tela "Ver envios" mostra.
    public record DetalheNotificacao(Notificacao notificacao, Aluno aluno, Turma turma,
            List<EnvioNotificacao> envios, boolean faltaAtiva) {
    }

    private INotificacaoDao notificacaoDao;
    private IEnvioNotificacaoDao envioDao;
    private IContatoResponsavelDao contatoDao;
    private IRegistroFrequenciaDao registroDao;
    private IMatriculaDao matriculaDao;
    private ITurmaDao turmaDao;
    private IAlunoDao alunoDao;
    private IEscolaDao escolaDao;
    private Clock relogio;

    public NotificacaoService(INotificacaoDao notificacaoDao, IEnvioNotificacaoDao envioDao,
            IContatoResponsavelDao contatoDao, IRegistroFrequenciaDao registroDao, IMatriculaDao matriculaDao,
            ITurmaDao turmaDao, IAlunoDao alunoDao, IEscolaDao escolaDao, Clock relogio) {
        this.notificacaoDao = notificacaoDao;
        this.envioDao = envioDao;
        this.contatoDao = contatoDao;
        this.registroDao = registroDao;
        this.matriculaDao = matriculaDao;
        this.turmaDao = turmaDao;
        this.alunoDao = alunoDao;
        this.escolaDao = escolaDao;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------
    // RN-60 e 61: a rotina das mensagens. No site roda sozinha a cada 30 s;
    // aqui é o botão "Processar mensagens". Recebe o "agora" para ser testável.
    // ------------------------------------------------------------------
    public ResultadoFila processarFila(LocalDateTime agora) {
        exigirLogado();

        // Etapa 1: avança as notificações que estão aguardando resposta
        int avancadas = 0;
        for (Notificacao n : notificacaoDao.consultarAguardando()) {
            List<ContatoResponsavel> fila = filaDeContatos(n.getIdMatricula());
            List<EnvioNotificacao> envios = envioDao.consultarPorNotificacao(n.getIdNotificacao());
            List<EnvioNotificacao> novos = avancar(n, envios, fila, agora);
            if (!novos.isEmpty() || n.getStatus() != StatusNotificacao.AGUARDANDO) {
                for (EnvioNotificacao e : novos) {
                    envioDao.cadastrar(e);
                }
                notificacaoDao.alterar(n, n.getIdNotificacao());
                avancadas++;
            }
        }

        // Etapa 2: cria as notificações do dia (só em dia útil)
        LocalDate dia = agora.toLocalDate();
        if (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return new ResultadoFila(0, avancadas);
        }
        int enviadas = 0;
        for (Escola escola : escolaDao.consultar()) {
            for (Turma turma : turmaDao.consultarPorEscola(escola.getCie(), dia.getYear())) {
                LocalTime envio = escola.getHorarioEnvio(turma.getTurno());
                if (envio == null || agora.isBefore(dia.atTime(envio))) {
                    continue; // turno sem horário de envio, ou o horário ainda não chegou
                }
                for (Matricula m : matriculaDao.consultarAtivasDaTurma(turma.getIdTurma())) {
                    if (!temFaltaAtiva(m.getIdMatricula(), dia)) {
                        continue;
                    }
                    if (notificacaoDao.consultarPorMatriculaData(m.getIdMatricula(), dia) != null) {
                        continue; // uma notificação por aluno por dia
                    }
                    criar(m, turma, escola, dia, agora);
                    enviadas++;
                }
            }
        }
        return new ResultadoFila(enviadas, avancadas);
    }

    // ------------------------------------------------------------------
    // RN-62: a falta foi cancelada (ou virou atraso): para as próximas tentativas.
    // Chamado pelo FrequenciaService.
    // ------------------------------------------------------------------
    public Notificacao interromperSeSemFalta(int idMatricula, LocalDate data) {
        Notificacao n = notificacaoDao.consultarPorMatriculaData(idMatricula, data);
        if (n == null) {
            return null;
        }
        if (temFaltaAtiva(idMatricula, data) || n.getStatus() != StatusNotificacao.AGUARDANDO) {
            return n;
        }
        n.setStatus(StatusNotificacao.INTERROMPIDA);
        n.setEncerradaEm(LocalDateTime.now(relogio));
        n.setObservacao("A falta foi cancelada pela secretaria. As próximas tentativas não serão enviadas.");
        notificacaoDao.alterar(n, n.getIdNotificacao());
        return n;
    }

    // ------------------------------------------------------------------
    // RN-63: registrar a resposta do responsável
    // ------------------------------------------------------------------
    public Notificacao registrarResposta(int idNotificacao, String texto) {
        String resposta = texto == null ? "" : texto.trim();
        if (resposta.isEmpty()) {
            throw new ValidacaoException("resposta", "Escreva a resposta recebida.");
        }
        Notificacao n = buscar(idNotificacao);
        if (n.getStatus() != StatusNotificacao.AGUARDANDO) {
            throw new RegraNegocioException("Esta notificação já foi encerrada.");
        }
        n.setStatus(StatusNotificacao.RESPONDIDA);
        n.setResposta(resposta);
        n.setRespondidaEm(LocalDateTime.now(relogio));
        notificacaoDao.alterar(n, idNotificacao);
        return n;
    }

    // ------------------------------------------------------------------
    // RN-64: "Simular 24 h". Volta 24 h no horário dos envios das que estão
    // aguardando e roda a fila. Devolve quantas notificações foram afetadas.
    // ------------------------------------------------------------------
    public int simularPassagemDe24h() {
        exigirLogado();
        int afetadas = 0;
        for (Notificacao n : notificacaoDao.consultarAguardando()) {
            if (!podeVer(n)) {
                continue; // a secretaria só mexe nas da escola dela
            }
            for (EnvioNotificacao e : envioDao.consultarPorNotificacao(n.getIdNotificacao())) {
                e.setEnviadoEm(e.getEnviadoEm().minusHours(PRAZO_RESPOSTA_HORAS));
                envioDao.alterar(e, e.getIdEnvio());
            }
            afetadas++;
        }
        processarFila(LocalDateTime.now(relogio));
        return afetadas;
    }

    // ------------------------------------------------------------------
    // Consultas para o menu
    // ------------------------------------------------------------------

    // Notificações que quem está logado pode ver. status null = todas.
    public List<ResumoNotificacao> listar(StatusNotificacao status) {
        exigirLogado();
        List<ResumoNotificacao> lista = new ArrayList<>();
        for (Notificacao n : notificacaoDao.consultar()) { // o DAO já ordena da mais nova para a mais antiga
            if (status != null && n.getStatus() != status) {
                continue;
            }
            Matricula m = matriculaDao.consultar(n.getIdMatricula());
            Turma turma = turmaDao.consultar(m.getIdTurma());
            if (!podeVerEscola(turma.getCie())) {
                continue;
            }
            lista.add(new ResumoNotificacao(n, alunoDao.consultar(m.getIdAluno()), turma));
        }
        return lista;
    }

    public DetalheNotificacao detalhe(int idNotificacao) {
        Notificacao n = buscar(idNotificacao);
        Matricula m = matriculaDao.consultar(n.getIdMatricula());
        return new DetalheNotificacao(n, alunoDao.consultar(m.getIdAluno()), turmaDao.consultar(m.getIdTurma()),
                envioDao.consultarPorNotificacao(idNotificacao), temFaltaAtiva(m.getIdMatricula(), n.getDataFalta()));
    }

    // ------------------------------------------------------------------
    // A regra da fila (fila.js). Não usa banco: recebe tudo e devolve os envios novos.
    // Altera o status da notificação quando a fila acaba.
    // ------------------------------------------------------------------
    public List<EnvioNotificacao> avancar(Notificacao n, List<EnvioNotificacao> envios,
            List<ContatoResponsavel> fila, LocalDateTime agora) {
        List<EnvioNotificacao> todos = new ArrayList<>(envios);
        List<EnvioNotificacao> novos = new ArrayList<>();

        while (n.getStatus() == StatusNotificacao.AGUARDANDO && !todos.isEmpty()) {
            EnvioNotificacao ultimo = todos.get(todos.size() - 1);
            // entregue: espera 24 h. Falhou: passa para o próximo na hora.
            LocalDateTime proximoEm = ultimo.isEntregue() ? ultimo.getEnviadoEm().plusHours(PRAZO_RESPOSTA_HORAS)
                    : ultimo.getEnviadoEm();
            if (agora.isBefore(proximoEm)) {
                break; // ainda está no prazo
            }

            int proximaPosicao = ultimo.getPosicao() + 1;
            if (proximaPosicao <= fila.size()) {
                EnvioNotificacao e = enviar(n.getIdNotificacao(), fila.get(proximaPosicao - 1), proximaPosicao,
                        proximoEm);
                todos.add(e);
                novos.add(e);
            } else {
                n.setStatus(StatusNotificacao.SEM_RESPOSTA);
                n.setEncerradaEm(proximoEm);
                n.setObservacao(ultimo.isEntregue()
                        ? "Sem resposta em " + PRAZO_RESPOSTA_HORAS + " h no último telefone da lista."
                        : "O envio falhou no último telefone da lista.");
            }
        }
        return novos;
    }

    // RN-64: simulação da API do WhatsApp. Número terminado em 0000 falha.
    public static boolean simularEntrega(String telefone) {
        return !telefone.endsWith("0000");
    }

    // RN-60: texto da mensagem
    public static String textoMensagem(Escola escola, Aluno aluno, Turma turma, LocalDate data) {
        String primeiroNome = aluno.getNomeAluno().trim().split("\\s+")[0];
        return "Olá! Aqui é a secretaria da " + escola.getNomeEscola() + ". " + primeiroNome + " ("
                + turma.getNomeTurma() + ") faltou hoje, " + data.format(BR)
                + ". Pode nos contar o motivo da ausência? É só responder esta mensagem.";
    }

    // ------------------------------------------------------------------
    // Métodos de apoio (private)
    // ------------------------------------------------------------------

    // RN-60 e 61: cria a notificação e faz o primeiro envio (ou já encerra, se não tem telefone)
    private void criar(Matricula m, Turma turma, Escola escola, LocalDate dia, LocalDateTime agora) {
        Aluno aluno = alunoDao.consultar(m.getIdAluno());
        List<ContatoResponsavel> fila = contatoDao.consultarPorAluno(m.getIdAluno());

        Notificacao n = new Notificacao();
        n.setIdMatricula(m.getIdMatricula());
        n.setDataFalta(dia);
        n.setMensagem(textoMensagem(escola, aluno, turma, dia));

        if (fila.isEmpty()) {
            n.setStatus(StatusNotificacao.SEM_RESPOSTA);
            n.setEncerradaEm(agora);
            n.setObservacao("O aluno não tem telefone de responsável cadastrado.");
            notificacaoDao.cadastrar(n);
            return;
        }

        n.setStatus(StatusNotificacao.AGUARDANDO);
        notificacaoDao.cadastrar(n); // preenche o id

        // primeiro envio vai para o Principal (posição 1)
        EnvioNotificacao primeiro = enviar(n.getIdNotificacao(), fila.get(0), 1, agora);
        envioDao.cadastrar(primeiro);

        // se o primeiro falhou, já tenta o próximo na hora
        List<EnvioNotificacao> envios = new ArrayList<>();
        envios.add(primeiro);
        List<EnvioNotificacao> novos = avancar(n, envios, fila, agora);
        for (EnvioNotificacao e : novos) {
            envioDao.cadastrar(e);
        }
        if (n.getStatus() != StatusNotificacao.AGUARDANDO) {
            notificacaoDao.alterar(n, n.getIdNotificacao());
        }
    }

    private EnvioNotificacao enviar(int idNotificacao, ContatoResponsavel contato, int posicao,
            LocalDateTime quando) {
        return new EnvioNotificacao(idNotificacao, contato.getIdContato(), posicao, contato.getTelefone(), quando,
                simularEntrega(contato.getTelefone()));
    }

    private List<ContatoResponsavel> filaDeContatos(int idMatricula) {
        Matricula m = matriculaDao.consultar(idMatricula);
        return contatoDao.consultarPorAluno(m.getIdAluno());
    }

    private boolean temFaltaAtiva(int idMatricula, LocalDate data) {
        for (RegistroFrequencia r : registroDao.consultarDoDia(idMatricula, data)) {
            if (r.getTipoOcorrencia() == TipoOcorrencia.FALTA && r.getStatus() == StatusRegistro.ATIVO) {
                return true;
            }
        }
        return false;
    }

    // Busca a notificação e confere a escola (RN-37)
    private Notificacao buscar(int idNotificacao) {
        exigirLogado();
        Notificacao n = notificacaoDao.consultar(idNotificacao);
        if (n == null) {
            throw new NaoEncontradoException("Notificação não encontrada.");
        }
        Matricula m = matriculaDao.consultar(n.getIdMatricula());
        SessaoUtil.exigirAcessoEscola(turmaDao.consultar(m.getIdTurma()).getCie());
        return n;
    }

    private boolean podeVer(Notificacao n) {
        Matricula m = matriculaDao.consultar(n.getIdMatricula());
        return podeVerEscola(turmaDao.consultar(m.getIdTurma()).getCie());
    }

    // Admin vê todas as escolas; a secretaria só a dela
    private boolean podeVerEscola(String cie) {
        Usuario u = SessaoUtil.getUsuario();
        return u.getPerfil() == Perfil.ADMIN || cie.equals(u.getCie());
    }

    // Admin e secretaria usam; só precisa ter alguém logado
    private Usuario exigirLogado() {
        Usuario u = SessaoUtil.getUsuario();
        if (u == null) {
            throw new AcessoNegadoException("Seu perfil não tem permissão para esta ação.");
        }
        return u;
    }
}

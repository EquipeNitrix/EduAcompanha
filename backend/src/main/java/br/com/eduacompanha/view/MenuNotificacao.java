package br.com.eduacompanha.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import br.com.eduacompanha.model.EnvioNotificacao;
import br.com.eduacompanha.model.Notificacao;
import br.com.eduacompanha.model.enums.StatusNotificacao;
import br.com.eduacompanha.service.NotificacaoService;
import br.com.eduacompanha.service.NotificacaoService.DetalheNotificacao;
import br.com.eduacompanha.service.NotificacaoService.ResultadoFila;
import br.com.eduacompanha.service.NotificacaoService.ResumoNotificacao;

// Menu das notificações (admin e secretaria). Nenhuma regra aqui: tudo está no NotificacaoService.
public class MenuNotificacao {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private NotificacaoService service;

    public MenuNotificacao(NotificacaoService service) {
        this.service = service;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Notificações", "Processar mensagens", "Listar", "Ver envios",
                    "Registrar resposta", "Simular 24 h", "Voltar");
            if (escolha == 5 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    processar();
                    break;
                case 1:
                    listar();
                    break;
                case 2:
                    verEnvios();
                    break;
                case 3:
                    registrarResposta();
                    break;
                case 4:
                    simular24h();
                    break;
            }
        }
    }

    private void processar() {
        try {
            ResultadoFila r = service.processarFila(LocalDateTime.now());
            Tela.mostrar(r.enviadas() + " notificação(ões) nova(s) enviada(s).\n" + r.avancadas()
                    + " notificação(ões) avançaram na fila.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void listar() {
        try {
            int filtro = Tela.escolher("Quais notificações?", "Aguardando", "Respondidas", "Sem resposta",
                    "Interrompidas", "Todas");
            if (filtro == -1) {
                return;
            }
            StatusNotificacao[] opcoes = { StatusNotificacao.AGUARDANDO, StatusNotificacao.RESPONDIDA,
                    StatusNotificacao.SEM_RESPOSTA, StatusNotificacao.INTERROMPIDA, null };

            String texto = "";
            for (ResumoNotificacao r : service.listar(opcoes[filtro])) {
                texto += String.format("%s  %-35s %-12s %s%n", r.notificacao().getDataFalta().format(DATA),
                        r.aluno().getNomeAluno(), r.turma().getNomeTurma(), r.notificacao().getStatus());
            }
            if (texto.equals("")) {
                texto = "Nenhuma notificação.";
            }
            Tela.mostrarLista("Notificações", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void verEnvios() {
        try {
            ResumoNotificacao escolhida = Tela.escolherDaLista("Qual notificação?", service.listar(null));
            if (escolhida == null) {
                return;
            }
            DetalheNotificacao d = service.detalhe(escolhida.notificacao().getIdNotificacao());
            Notificacao n = d.notificacao();

            String texto = "Aluno: " + d.aluno().getNomeAluno() + " (" + d.turma().getNomeTurma() + ")\n";
            texto += "Falta em: " + n.getDataFalta().format(DATA)
                    + (d.faltaAtiva() ? "" : "  (a falta foi cancelada)") + "\n";
            texto += "Situação: " + n.getStatus() + "\n\n";
            texto += "Mensagem:\n" + n.getMensagem() + "\n\n";

            texto += "ENVIOS\n";
            if (d.envios().isEmpty()) {
                texto += "Nenhum envio.\n";
            }
            for (EnvioNotificacao e : d.envios()) {
                texto += String.format("%dº telefone  %-16s %s  %s%n", e.getPosicao(), e.getTelefoneEnviado(),
                        e.getEnviadoEm().format(DATA_HORA), e.isEntregue() ? "entregue" : "FALHOU");
            }
            if (n.getResposta() != null) {
                texto += "\nResposta (" + n.getRespondidaEm().format(DATA_HORA) + "): " + n.getResposta() + "\n";
            }
            if (n.getObservacao() != null && !n.getObservacao().isEmpty()) {
                texto += "\nObservação: " + n.getObservacao() + "\n";
            }
            Tela.mostrarLista("Notificação", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void registrarResposta() {
        try {
            List<ResumoNotificacao> aguardando = service.listar(StatusNotificacao.AGUARDANDO);
            ResumoNotificacao escolhida = Tela.escolherDaLista("Qual notificação?", aguardando);
            if (escolhida == null) {
                return;
            }
            String texto = Tela.pedirTexto("Resposta recebida do responsável:");
            if (texto == null) {
                return;
            }
            service.registrarResposta(escolhida.notificacao().getIdNotificacao(), texto);
            Tela.mostrar("Resposta registrada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void simular24h() {
        try {
            int afetadas = service.simularPassagemDe24h();
            Tela.mostrar("Simulação feita: " + afetadas + " notificação(ões) aguardando passaram 24 h.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }
}

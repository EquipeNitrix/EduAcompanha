package br.com.eduacompanha.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.AcompanhamentoCaso;
import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.CasoBuscaAtiva;
import br.com.eduacompanha.model.Responsavel;
import br.com.eduacompanha.model.enums.StatusCaso;
import br.com.eduacompanha.model.enums.TipoRegistro;
import br.com.eduacompanha.service.BuscaAtivaService;

// Menu da Busca Ativa. Só pergunta, chama o Service e mostra a resposta.
// Nenhuma regra aqui: as regras estão no BuscaAtivaService.
public class MenuBuscaAtiva {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private BuscaAtivaService service;

    public MenuBuscaAtiva(BuscaAtivaService service) {
        this.service = service;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Busca Ativa", "Listar casos", "Abrir caso", "Ver caso",
                    "Registrar tentativa", "Registrar ação", "Encerrar", "Voltar");
            if (escolha == 6 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    listar();
                    break;
                case 1:
                    abrirCaso();
                    break;
                case 2:
                    verCaso();
                    break;
                case 3:
                    registrarTentativa();
                    break;
                case 4:
                    registrarAcao();
                    break;
                case 5:
                    encerrar();
                    break;
            }
        }
    }

    private void listar() {
        try {
            int filtro = Tela.escolher("Quais casos?", "Abertos", "Encerrados", "Todos");
            if (filtro == -1) {
                return;
            }
            StatusCaso status = null;
            if (filtro == 0) {
                status = StatusCaso.ABERTO;
            } else if (filtro == 1) {
                status = StatusCaso.ENCERRADO;
            }

            String texto = "";
            for (BuscaAtivaService.ResumoCaso r : service.listar(status)) {
                texto += String.format("#%-4d %-35s %-10s aberto em %s   %d tentativa(s)%n",
                        r.caso().getIdCaso(), r.aluno().getNomeAluno(), r.caso().getStatus(),
                        r.caso().getDataAbertura().format(DATA), r.tentativas());
            }
            if (texto.equals("")) {
                texto = "Nenhum caso.";
            }
            Tela.mostrarLista("Casos de Busca Ativa", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void abrirCaso() {
        try {
            List<Aluno> candidatos = service.candidatos();
            List<String> nomes = new ArrayList<>();
            for (Aluno a : candidatos) {
                nomes.add(a.getNomeAluno() + " (RA " + a.getRa() + ")");
            }
            String nome = Tela.escolherDaLista("Escolha o aluno:", nomes);
            if (nome == null) {
                return;
            }
            Aluno aluno = candidatos.get(nomes.indexOf(nome));

            LocalDate abertura = pedirData("Data de abertura (dd/mm/aaaa):", LocalDate.now());
            if (abertura == null) {
                return;
            }
            String motivo = Tela.pedirTexto("Motivo (opcional):");
            if (motivo == null) {
                return;
            }

            CasoBuscaAtiva caso = service.abrirCaso(aluno.getIdAluno(), abertura, motivo);
            Tela.mostrar("Caso #" + caso.getIdCaso() + " aberto.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void verCaso() {
        try {
            BuscaAtivaService.ResumoCaso escolhido = Tela.escolherDaLista("Qual caso?", service.listar(null));
            if (escolhido == null) {
                return;
            }
            BuscaAtivaService.DetalheCaso d = service.detalhe(escolhido.caso().getIdCaso());
            CasoBuscaAtiva c = d.caso();

            String texto = "Aluno: " + d.aluno().getNomeAluno() + " (RA " + d.aluno().getRa() + ")\n";
            texto += "Situação: " + c.getStatus() + "\n";
            texto += "Aberto em: " + c.getDataAbertura().format(DATA) + "\n";
            texto += "Motivo: " + (c.getMotivo() == null ? "-" : c.getMotivo()) + "\n";
            if (c.getStatus() == StatusCaso.ENCERRADO) {
                texto += "Encerrado em: " + c.getDataEncerramento().format(DATA) + "\n";
                texto += "Desfecho: " + c.getDesfecho() + "\n";
                texto += "Observação: " + (c.getObservacaoFinal() == null ? "-" : c.getObservacaoFinal()) + "\n";
            }

            texto += "\nLINHA DO TEMPO\n";
            if (d.registros().isEmpty()) {
                texto += "Nenhum registro ainda.\n";
            }
            for (AcompanhamentoCaso a : d.registros()) {
                texto += a.getDataHora().format(DATA_HORA) + "  ";
                if (a.getTipoRegistro() == TipoRegistro.TENTATIVA) {
                    texto += "TENTATIVA | " + a.getCanal() + " | " + a.getResultado();
                    String resp = nomeDoResponsavel(d.responsaveis(), a.getIdResponsavel());
                    if (resp != null) {
                        texto += " | com " + resp;
                    }
                } else {
                    texto += "AÇÃO | " + a.getTipoAcao();
                }
                texto += "\n      " + a.getDescricao() + "\n";
            }
            Tela.mostrarLista("Caso #" + c.getIdCaso(), texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void registrarTentativa() {
        try {
            BuscaAtivaService.ResumoCaso r = escolherCasoAberto();
            if (r == null) {
                return;
            }
            int idCaso = r.caso().getIdCaso();

            String canal = Tela.escolherDaLista("Canal:", BuscaAtivaService.CANAIS);
            if (canal == null) {
                return;
            }
            LocalDateTime dataHora = pedirDataHora("Data e hora (dd/mm/aaaa hh:mm):");
            if (dataHora == null) {
                return;
            }
            String resultado = Tela.escolherDaLista("Resultado:", BuscaAtivaService.RESULTADOS);
            if (resultado == null) {
                return;
            }

            // Responsável é opcional: a primeira opção é "não informar".
            List<Responsavel> responsaveis = service.responsaveisDoCaso(idCaso);
            List<String> opcoes = new ArrayList<>();
            opcoes.add("Não informar");
            for (Responsavel resp : responsaveis) {
                opcoes.add(resp.getNomeResponsavel() + " (" + resp.getParentesco() + ")");
            }
            String opcao = Tela.escolherDaLista("Com quem falou?", opcoes);
            if (opcao == null) {
                return;
            }
            Integer idResponsavel = null;
            int pos = opcoes.indexOf(opcao);
            if (pos > 0) {
                idResponsavel = responsaveis.get(pos - 1).getIdResponsavel();
            }

            String descricao = Tela.pedirTexto("Descreva a conversa:");
            if (descricao == null) {
                return;
            }

            service.registrarTentativa(idCaso, canal, dataHora, descricao, resultado, idResponsavel);
            Tela.mostrar("Tentativa registrada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void registrarAcao() {
        try {
            BuscaAtivaService.ResumoCaso r = escolherCasoAberto();
            if (r == null) {
                return;
            }
            String tipo = Tela.escolherDaLista("Tipo de ação:", BuscaAtivaService.TIPOS_ACAO);
            if (tipo == null) {
                return;
            }
            LocalDateTime dataHora = pedirDataHora("Data e hora (dd/mm/aaaa hh:mm):");
            if (dataHora == null) {
                return;
            }
            String descricao = Tela.pedirTexto("Descreva a ação:");
            if (descricao == null) {
                return;
            }

            service.registrarAcao(r.caso().getIdCaso(), tipo, dataHora, descricao);
            Tela.mostrar("Ação registrada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void encerrar() {
        try {
            BuscaAtivaService.ResumoCaso r = escolherCasoAberto();
            if (r == null) {
                return;
            }
            LocalDate data = pedirData("Data do encerramento (dd/mm/aaaa):", LocalDate.now());
            if (data == null) {
                return;
            }
            String desfecho = Tela.escolherDaLista("Desfecho:", BuscaAtivaService.DESFECHOS);
            if (desfecho == null) {
                return;
            }
            String obs = Tela.pedirTexto("Observação final (opcional):");
            if (obs == null) {
                return;
            }

            service.encerrar(r.caso().getIdCaso(), data, desfecho, obs);
            Tela.mostrar("Caso encerrado.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    // ------------------------------------------------------------------
    // Ajudantes só deste menu
    // ------------------------------------------------------------------

    private BuscaAtivaService.ResumoCaso escolherCasoAberto() {
        return Tela.escolherDaLista("Qual caso?", service.listar(StatusCaso.ABERTO));
    }

    private String nomeDoResponsavel(List<Responsavel> responsaveis, Integer id) {
        if (id == null) {
            return null;
        }
        for (Responsavel r : responsaveis) {
            if (r.getIdResponsavel() == id) {
                return r.getNomeResponsavel();
            }
        }
        return null;
    }

    // Pede uma data no formato dd/MM/yyyy e repete enquanto estiver errada. null =
    // Cancelar.
    private LocalDate pedirData(String rotulo, LocalDate sugestao) {
        while (true) {
            String resposta = Tela.pedirTexto(rotulo, sugestao == null ? null : sugestao.format(DATA));
            if (resposta == null) {
                return null;
            }
            try {
                return LocalDate.parse(resposta, DATA);
            } catch (DateTimeParseException e) {
                Tela.mostrar("Use o formato dd/mm/aaaa, por exemplo 17/11/2026.");
            }
        }
    }

    // Igual, com hora: dd/MM/yyyy HH:mm. Já vem preenchido com agora.
    private LocalDateTime pedirDataHora(String rotulo) {
        String agora = LocalDateTime.now().format(DATA_HORA);
        while (true) {
            String resposta = Tela.pedirTexto(rotulo, agora);
            if (resposta == null) {
                return null;
            }
            try {
                return LocalDateTime.parse(resposta, DATA_HORA);
            } catch (DateTimeParseException e) {
                Tela.mostrar("Use o formato dd/mm/aaaa hh:mm, por exemplo 17/11/2026 14:30.");
            }
        }
    }
}

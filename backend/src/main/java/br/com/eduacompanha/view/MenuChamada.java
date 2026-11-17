package br.com.eduacompanha.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import br.com.eduacompanha.model.AlunoFrequencia;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.TipoOcorrencia;
import br.com.eduacompanha.service.FrequenciaService;
import br.com.eduacompanha.service.FrequenciaService.DadosChamada;
import br.com.eduacompanha.service.FrequenciaService.LinhaChamada;
import br.com.eduacompanha.service.FrequenciaService.OcorrenciaDoDia;

// Menu da chamada (secretaria). Escolhe turma e data, mostra a lista do dia
// e lança as ocorrências. Nenhuma regra aqui: tudo está no FrequenciaService.
public class MenuChamada {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FrequenciaService service;

    public MenuChamada(FrequenciaService service) {
        this.service = service;
    }

    // 1º: escolhe a turma e a data. 2º: abre a chamada daquela turma naquele dia.
    public void abrir() {
        while (true) {
            try {
                Turma turma = Tela.escolherDaLista("Escolha a turma:", service.turmasParaChamada());
                if (turma == null) {
                    return;
                }
                LocalDate data = pedirData("Data da chamada (dd/mm/aaaa):", LocalDate.now());
                if (data == null) {
                    continue;
                }
                chamada(turma.getIdTurma(), data);
            } catch (Exception e) {
                Tela.erro(e);
                return;
            }
        }
    }

    // Menu de botões da chamada de uma turma num dia
    private void chamada(int idTurma, LocalDate data) {
        while (true) {
            int escolha = Tela.escolher("Chamada de " + data.format(DATA), "Ver lista", "Falta", "Atraso", "Saída",
                    "Registrar volta", "Chegou atrasado", "Cancelar ocorrência", "Voltar");
            if (escolha == 7 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    verLista(idTurma, data);
                    break;
                case 1:
                    lancar(idTurma, data, TipoOcorrencia.FALTA);
                    break;
                case 2:
                    lancar(idTurma, data, TipoOcorrencia.ATRASO);
                    break;
                case 3:
                    lancar(idTurma, data, TipoOcorrencia.SAIDA_ANTECIPADA);
                    break;
                case 4:
                    registrarVolta(idTurma, data);
                    break;
                case 5:
                    chegouAtrasado(idTurma, data);
                    break;
                case 6:
                    cancelar(idTurma, data);
                    break;
            }
        }
    }

    private void verLista(int idTurma, LocalDate data) {
        try {
            DadosChamada dados = service.dadosDaChamada(idTurma, data);
            String texto = dados.turma().getNomeTurma() + " - " + data.format(DATA) + "\n";
            if (dados.aulasDoDia() == null) {
                texto += "Turno sem grade de horários.\n\n";
            } else {
                texto += dados.aulasDoDia().size() + " aula(s) neste dia\n\n";
            }

            for (LinhaChamada l : dados.linhas()) {
                texto += String.format("%-35s freq. %s%n", l.aluno().getNomeAluno(), percentual(l.frequencia()));
                for (OcorrenciaDoDia o : l.ocorrencias()) {
                    texto += "     " + FrequenciaService.descrever(o.registro()) + " (" + o.aulasPerdidas()
                            + " aula(s) perdida(s))\n";
                }
            }
            if (dados.linhas().isEmpty()) {
                texto += "Nenhum aluno matriculado nesta data.";
            }
            Tela.mostrarLista("Chamada", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    // Falta, atraso ou saída: mesma receita, muda só quais horas perguntar
    private void lancar(int idTurma, LocalDate data, TipoOcorrencia tipo) {
        try {
            DadosChamada dados = service.dadosDaChamada(idTurma, data);
            LinhaChamada linha = Tela.escolherDaLista("Qual aluno?", dados.linhas());
            if (linha == null) {
                return;
            }

            LocalTime entrada = null;
            LocalTime saida = null;
            if (tipo == TipoOcorrencia.ATRASO) {
                entrada = Tela.pedirHora("Hora em que o aluno chegou (HH:MM):");
                if (entrada == null) {
                    return;
                }
            }
            if (tipo == TipoOcorrencia.SAIDA_ANTECIPADA) {
                saida = Tela.pedirHora("Hora em que o aluno saiu (HH:MM):");
                if (saida == null) {
                    return;
                }
            }
            String obs = Tela.pedirTexto("Observação (opcional):");
            if (obs == null) {
                return;
            }

            service.lancar(linha.matricula().getIdMatricula(), data, tipo, entrada, saida, obs);
            Tela.mostrar("Ocorrência registrada para " + linha.aluno().getNomeAluno() + ".");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void registrarVolta(int idTurma, LocalDate data) {
        try {
            OcorrenciaDoDia o = escolherOcorrencia(idTurma, data, "Qual saída?");
            if (o == null) {
                return;
            }
            LocalTime volta = Tela.pedirHora("Hora em que o aluno voltou (HH:MM):");
            if (volta == null) {
                return;
            }
            service.registrarVolta(o.registro().getIdRegistroFreq(), volta);
            Tela.mostrar("Volta registrada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void chegouAtrasado(int idTurma, LocalDate data) {
        try {
            OcorrenciaDoDia o = escolherOcorrencia(idTurma, data, "Qual falta?");
            if (o == null) {
                return;
            }
            LocalTime entrada = Tela.pedirHora("Hora em que o aluno chegou (HH:MM):");
            if (entrada == null) {
                return;
            }
            service.chegouAtrasado(o.registro().getIdRegistroFreq(), entrada);
            Tela.mostrar("A falta virou atraso.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void cancelar(int idTurma, LocalDate data) {
        try {
            OcorrenciaDoDia o = escolherOcorrencia(idTurma, data, "Qual ocorrência cancelar?");
            if (o == null) {
                return;
            }
            String motivo = Tela.pedirTexto("Motivo do cancelamento:");
            if (motivo == null) {
                return;
            }
            service.cancelar(o.registro().getIdRegistroFreq(), motivo);
            Tela.mostrar("Ocorrência cancelada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    // ------------------------------------------------------------------
    // Ajudantes só deste menu
    // ------------------------------------------------------------------

    // Mostra as ocorrências do dia (o toString de cada uma) para escolher uma
    private OcorrenciaDoDia escolherOcorrencia(int idTurma, LocalDate data, String titulo) {
        List<OcorrenciaDoDia> ocorrencias = service.dadosDaChamada(idTurma, data).ocorrencias();
        return Tela.escolherDaLista(titulo, ocorrencias);
    }

    private String percentual(AlunoFrequencia f) {
        if (f.getPercentual() == null) {
            return "-";
        }
        return String.format("%.1f%%", f.getPercentual());
    }

    // Pede uma data no formato dd/MM/yyyy e repete enquanto estiver errada. null = Cancelar.
    private LocalDate pedirData(String rotulo, LocalDate sugestao) {
        while (true) {
            String resposta = Tela.pedirTexto(rotulo, sugestao.format(DATA));
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
}

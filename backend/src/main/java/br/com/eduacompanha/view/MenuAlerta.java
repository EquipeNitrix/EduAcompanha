package br.com.eduacompanha.view;

import java.util.List;

import br.com.eduacompanha.model.AlunoFrequencia;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.service.AlertaService;
import br.com.eduacompanha.service.EscolaService;

// Menu de alerta de evasão (admin e secretaria).
// Mostra quem está abaixo de 80% e os padrões de falta de um aluno.
public class MenuAlerta {
    private EscolaService escolaService;
    private AlertaService alertaService;

    public MenuAlerta(EscolaService escolaService, AlertaService alertaService) {
        this.escolaService = escolaService;
        this.alertaService = alertaService;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Alerta de evasão", "Alunos em alerta", "Padrões de falta", "Voltar");
            if (escolha == 2 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    listar();
                    break;
                case 1:
                    padroes();
                    break;
            }
        }
    }

    private void listar() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            List<AlunoFrequencia> lista = alertaService.alunosEmAlerta(escola.getCie(), null);

            String texto = "Crítico: abaixo de 75%.  Atenção: de 75% até abaixo de 80%.\n\n";
            for (AlunoFrequencia f : lista) {
                texto += String.format("%-9s %6.1f%%  %-35s %-10s %d dia(s) de falta%n",
                        rotulo(AlertaService.nivel(f.getPercentual())), f.getPercentual(),
                        f.getAluno().getNomeAluno(), f.getTurma().getNomeTurma(), f.getDiasComFalta());
            }
            if (lista.isEmpty()) {
                texto += "Nenhum aluno em alerta.";
            }
            Tela.mostrarLista("Alunos em alerta - " + escola.getNomeEscola(), texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void padroes() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            List<AlunoFrequencia> alunos = alertaService.indicadores(escola.getCie(), null);
            if (alunos.isEmpty()) {
                Tela.mostrar("Nenhum aluno com matrícula ativa.");
                return;
            }
            String[] nomes = new String[alunos.size()];
            for (int i = 0; i < alunos.size(); i++) {
                nomes[i] = alunos.get(i).getAluno().getNomeAluno() + " (" + alunos.get(i).getTurma().getNomeTurma()
                        + ")";
            }
            String nome = Tela.escolherDaLista("Qual aluno?", List.of(nomes));
            if (nome == null) {
                return;
            }
            AlunoFrequencia escolhido = alunos.get(List.of(nomes).indexOf(nome));

            List<String> padroes = alertaService.padroesDeFalta(escolhido.getAluno().getIdAluno());
            if (padroes.isEmpty()) {
                Tela.mostrar("Nenhum padrão de falta nos últimos 30 dias.");
            } else {
                Tela.mostrar(String.join("\n", padroes));
            }
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    // Admin escolhe a escola; a secretaria só tem a dela, então nem pergunta.
    private Escola escolherEscola() {
        List<Escola> escolas = escolaService.listarAcessiveis();
        if (escolas.size() == 1) {
            return escolas.get(0);
        }
        return Tela.escolherDaLista("Escolha a escola:", escolas);
    }

    private String rotulo(String nivel) {
        return "critico".equals(nivel) ? "CRÍTICO" : "ATENÇÃO";
    }
}

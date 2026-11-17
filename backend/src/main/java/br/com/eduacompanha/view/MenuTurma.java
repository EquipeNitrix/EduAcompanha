package br.com.eduacompanha.view;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.TurmaService;

public class MenuTurma {
    private EscolaService escolaService;
    private TurmaService turmaService;

    public MenuTurma(EscolaService escolaService, TurmaService turmaService) {
        this.escolaService = escolaService;
        this.turmaService = turmaService;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Turmas", "Listar", "Cadastrar", "Editar", "Voltar");
            if (escolha == 3 || escolha == -1) {
                return;
            } else {
                switch (escolha) {
                    case 0:
                        listar();
                        break;
                    case 1:
                        cadastrar();
                        break;

                    case 2:
                        editar();
                        break;
                }
            }
        }
    }

    private void listar() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            List<Turma> turmas = turmaService.listar(escola.getCie());
            String texto = "";
            for (Turma t : turmas) {
                texto += t.getNomeTurma() + " | " + t.getTurno() + "\n";
            }
            Tela.mostrarLista("Turmas de " + escola.getNomeEscola(), texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void cadastrar() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            String nome = Tela.pedirTexto("Informe o nome da turma:");
            if (nome == null) {
                return;
            }
            Turno turno = Tela.escolherDaLista("Escolha o turno.", escola.getModeloTurno().turnos());
            if (turno == null) {
                return;
            }
            Turma turma = new Turma();
            turma.setNomeTurma(nome);
            turma.setTurno(turno);
            turma.setCie(escola.getCie());

            turmaService.salvar(turma);
            Tela.mostrar("Turma cadastrada.");

        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void editar() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            List<Turma> turmas = turmaService.listar(escola.getCie());
            Turma turma = Tela.escolherDaLista("Qual a turma?", turmas);
            if (turma == null) {
                return;
            }

            String nome = Tela.pedirTexto("Informe o nome da turma:", turma.getNomeTurma());
            if (nome == null) {
                return;
            }

            Turno turno = Tela.escolherDaLista("Escolha o turno (atual: " + turma.getTurno() + ")",
                    escola.getModeloTurno().turnos());
            if (turno == null) {
                return;
            }

            turma.setNomeTurma(nome);
            turma.setTurno(turno);

            turmaService.salvar(turma);
            Tela.mostrar("Turma atualizada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private Escola escolherEscola() {
        List<Escola> escolas = escolaService.listarAcessiveis();

        if (escolas.size() == 1) {
            return escolas.get(0);
        }
        return Tela.escolherDaLista("Escolha a escola", escolas);
    }

}

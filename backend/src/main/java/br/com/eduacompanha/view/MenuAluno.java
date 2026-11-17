package br.com.eduacompanha.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Responsavel;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.StatusMatricula;
import br.com.eduacompanha.service.AlunoService;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.MatriculaService;
import br.com.eduacompanha.service.TurmaService;

public class MenuAluno {
    private EscolaService escolaService;
    private TurmaService turmaService;
    private MatriculaService matriculaService;
    private AlunoService alunoService;

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MenuAluno(EscolaService escolaService, TurmaService turmaService, MatriculaService matriculaService,
            AlunoService alunoService) {
        this.escolaService = escolaService;
        this.turmaService = turmaService;
        this.matriculaService = matriculaService;
        this.alunoService = alunoService;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Alunos", "Buscar", "Cadastrar", "Editar", "Matricular",
                    "Encerrar matrícula", "Voltar");
            if (escolha == 5 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    buscar();
                    break;
                case 1:
                    cadastrar();
                    break;
                case 2:
                    editar();
                    break;
                case 3:
                    matricular();
                    break;
                case 4:
                    encerrarMatricula();
                    break;
            }
        }
    }

    private void buscar() {
        try {
            String trecho = Tela.pedirTexto("Nome do aluno (ou parte do nome):");
            if (trecho == null) {
                return;
            }
            List<Aluno> alunos = alunoService.buscar(trecho);
            if (alunos.isEmpty()) {
                Tela.mostrar("Nenhum aluno encontrado.");
                return;
            }
            String texto = "";
            for (Aluno a : alunos) {
                texto += a.getNomeAluno() + " | RA " + a.getRa() + "\n";
            }
            Tela.mostrarLista("Alunos encontrados (" + alunos.size() + ")", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void cadastrar() {
        try {
            Turma turma = escolherTurma();
            if (turma == null) {
                return;
            }

            String ra = Tela.pedirTexto("RA do aluno (ex.: 000123456789-0):");
            if (ra == null) {
                return;
            }
            String nome = Tela.pedirTexto("Nome completo do aluno:");
            if (nome == null) {
                return;
            }
            LocalDate inicio = pedirData("Data de início da matrícula (dd/mm/aaaa):");
            if (inicio == null) {
                return;
            }

            String nomeResponsavel = Tela.pedirTexto("Nome do responsável:");
            if (nomeResponsavel == null) {
                return;
            }
            String parentesco = Tela.escolherDaLista("Parentesco do responsável:", AlunoService.PARENTESCOS);
            if (parentesco == null) {
                return;
            }
            String telefone1 = Tela.pedirTexto("Telefone principal (ex.: (15) 99999-9999):");
            if (telefone1 == null) {
                return;
            }
            String telefone2 = Tela.pedirTexto("Telefone secundário (deixe vazio se não tiver):");
            if (telefone2 == null) {
                return;
            }

            Aluno aluno = new Aluno(ra, nome);

            Matricula matricula = new Matricula();
            matricula.setIdTurma(turma.getIdTurma());
            matricula.setDataInicio(inicio);

            Responsavel responsavel = new Responsavel();
            responsavel.setNomeResponsavel(nomeResponsavel);
            responsavel.setParentesco(parentesco);

            Aluno salvo = alunoService.cadastrar(aluno, matricula, responsavel, telefone1, telefone2);
            Tela.mostrar("Aluno " + salvo.getNomeAluno() + " cadastrado na turma " + turma.getNomeTurma() + ".");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void editar() {
        try {
            Aluno aluno = escolherAluno();
            if (aluno == null) {
                return;
            }
            String ra = Tela.pedirTexto("RA do aluno:", aluno.getRa());
            if (ra == null) {
                return;
            }
            String nome = Tela.pedirTexto("Nome completo do aluno:", aluno.getNomeAluno());
            if (nome == null) {
                return;
            }
            alunoService.editar(aluno.getIdAluno(), ra, nome);
            Tela.mostrar("Aluno atualizado.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void matricular() {
        try {
            Aluno aluno = escolherAluno();
            if (aluno == null) {
                return;
            }
            Turma turma = escolherTurma();
            if (turma == null) {
                return;
            }
            LocalDate inicio = pedirData("Data de início da matrícula (dd/mm/aaaa):");
            if (inicio == null) {
                return;
            }
            matriculaService.matricular(aluno.getIdAluno(), turma.getIdTurma(), inicio);
            Tela.mostrar(aluno.getNomeAluno() + " matriculado(a) na turma " + turma.getNomeTurma() + ".");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void encerrarMatricula() {
        try {
            Aluno aluno = escolherAluno();
            if (aluno == null) {
                return;
            }

            // só as matrículas ativas entram na lista
            List<Matricula> ativas = new ArrayList<>();
            for (Matricula m : matriculaService.doAluno(aluno.getIdAluno())) {
                if (m.getStatus() == StatusMatricula.ATIVA) {
                    ativas.add(m);
                }
            }
            if (ativas.isEmpty()) {
                Tela.mostrar("Este aluno não tem matrícula ativa.");
                return;
            }

            Matricula matricula = ativas.get(0);
            if (ativas.size() > 1) {
                List<String> opcoes = new ArrayList<>();
                for (Matricula m : ativas) {
                    opcoes.add("Ativa desde " + BR.format(m.getDataInicio()));
                }
                String escolhida = Tela.escolherDaLista("Qual matrícula encerrar?", opcoes);
                if (escolhida == null) {
                    return;
                }
                matricula = ativas.get(opcoes.indexOf(escolhida));
            }

            LocalDate fim = pedirData("Data em que o aluno saiu (dd/mm/aaaa):");
            if (fim == null) {
                return;
            }
            int resposta = Tela.escolher("Encerrar a matrícula de " + aluno.getNomeAluno() + " em "
                    + BR.format(fim) + "?", "Sim", "Não");
            if (resposta != 0) {
                return;
            }
            matriculaService.encerrar(matricula.getIdMatricula(), fim);
            Tela.mostrar("Matrícula encerrada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private Aluno escolherAluno() {
        String trecho = Tela.pedirTexto("Nome do aluno (ou parte do nome):");
        if (trecho == null) {
            return null;
        }
        List<Aluno> alunos = alunoService.buscar(trecho);
        if (alunos.isEmpty()) {
            Tela.mostrar("Nenhum aluno encontrado.");
            return null;
        }
        return Tela.escolherDaLista("Escolha o aluno", alunos);
    }

    private Turma escolherTurma() {
        Escola escola = escolherEscola();
        if (escola == null) {
            return null;
        }
        List<Turma> turmas = turmaService.listar(escola.getCie());
        if (turmas.isEmpty()) {
            Tela.mostrar("Esta escola ainda não tem turmas neste ano.");
            return null;
        }
        return Tela.escolherDaLista("Escolha a turma", turmas);
    }

    private LocalDate pedirData(String rotulo) {
        String texto = Tela.pedirTexto(rotulo);
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), BR);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida. Use o formato dd/mm/aaaa, por exemplo 23/11/2026.");
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

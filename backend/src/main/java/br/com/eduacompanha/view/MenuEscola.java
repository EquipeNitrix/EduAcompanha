package br.com.eduacompanha.view;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.enums.ModeloTurno;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.service.EscolaService;

public class MenuEscola {
    private EscolaService escolaService;

    public MenuEscola(EscolaService escolaService) {
        this.escolaService = escolaService;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Escolas", "Listar", "Cadastrar", "Editar", "Voltar");
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
            List<EscolaService.Resumo> resumos = escolaService.listarResumo();
            String texto = "";
            for (EscolaService.Resumo r : resumos) {
                texto += r.escola().getCie() + " | " + r.escola().getNomeEscola() + " | "
                        + (r.escola().getMunicipio() == null ? "-" : r.escola().getMunicipio()) + " | " + r.modelo()
                        + " | " + r.usuarios() + " Usuários | " + r.turmas() + " Turmas | " + r.turnosPendentes()
                        + " Turnos pendentes" + "\n";
            }
            Tela.mostrarLista("Escolas", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void cadastrar() {
        String cie = Tela.pedirTexto("Informe o CIE da escola:");
        if (cie == null) {
            return;
        }
        String nome = Tela.pedirTexto("Informe o nome da escola:");
        if (nome == null) {
            return;
        }
        String municipio = Tela.pedirTexto("Informe o município da escola (opcional): ");
        if (municipio == null) {
            return;
        }

        int escolha = Tela.escolher("Modelo turno", "Parcial", "Integral");
        if (escolha == -1) {
            return;
        }
        ModeloTurno modelo = (escolha == 0 ? ModeloTurno.PARCIAL : ModeloTurno.INTEGRAL);

        Escola escola = new Escola();
        escola.setCie(cie);
        escola.setNomeEscola(nome);
        escola.setMunicipio(municipio);
        escola.setModeloTurno(modelo);

        if (modelo == ModeloTurno.INTEGRAL) {
            LocalTime integral = Tela.pedirHora("Informe a hora de envio (opcional):");
            escola.setHorarioEnvio(Turno.INTEGRAL, integral);
        } else {
            LocalTime manha = Tela.pedirHora("Informe a hora de envio da manhã (opcional):");
            escola.setHorarioEnvio(Turno.MANHA, manha);
            LocalTime tarde = Tela.pedirHora("Informe a hora de envio da tarde (opcional):");
            escola.setHorarioEnvio(Turno.TARDE, tarde);
            LocalTime noite = Tela.pedirHora("Informe a hora de envio da noite (opcional):");
            escola.setHorarioEnvio(Turno.NOITE, noite);
        }

        try {
            escolaService.salvar(escola, true);
            Tela.mostrar("Escola cadastrada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void editar() {
        try {
            List<EscolaService.Resumo> resumos = escolaService.listarResumo();

            List<Escola> escolas = new ArrayList<>();
            for (EscolaService.Resumo r : resumos) {
                escolas.add(r.escola());
            }

            Escola escola = Tela.escolherDaLista("Qual escola?", escolas);
            if (escola == null) {
                return;
            }

            String nome = Tela.pedirTexto("Informe o nome da escola:", escola.getNomeEscola());
            if (nome == null) {
                return;
            }
            escola.setNomeEscola(nome);
            String municipio = Tela.pedirTexto("Informe o município da escola (opcional): ", escola.getMunicipio());
            if (municipio == null) {
                return;
            }
            escola.setMunicipio(municipio);

            int escolha = Tela.escolher("Modelo de turno (atual: " + escola.getModeloTurno() + ")", "Parcial",
                    "Integral");
            if (escolha == -1) {
                return;
            }
            escola.setModeloTurno(escolha == 0 ? ModeloTurno.PARCIAL : ModeloTurno.INTEGRAL);

            if (escola.getModeloTurno() == ModeloTurno.INTEGRAL) {
                LocalTime integral = Tela.pedirHora("Informe a hora de envio (opcional):",
                        escola.getHorarioEnvioIntegral());
                escola.setHorarioEnvio(Turno.INTEGRAL, integral);
            } else {
                LocalTime manha = Tela.pedirHora("Informe a hora de envio da manhã (opcional):",
                        escola.getHorarioEnvioManha());
                escola.setHorarioEnvio(Turno.MANHA, manha);
                LocalTime tarde = Tela.pedirHora("Informe a hora de envio da tarde (opcional):",
                        escola.getHorarioEnvioTarde());
                escola.setHorarioEnvio(Turno.TARDE, tarde);
                LocalTime noite = Tela.pedirHora("Informe a hora de envio da noite (opcional):",
                        escola.getHorarioEnvioNoite());
                escola.setHorarioEnvio(Turno.NOITE, noite);
            }

            escolaService.salvar(escola, false);
            Tela.mostrar("Escola atualizada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }
}

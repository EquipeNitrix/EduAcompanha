package br.com.eduacompanha.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.TurnoService;

public class MenuTurno {
    private EscolaService escolaService;
    private TurnoService turnoService;

    public MenuTurno(EscolaService escolaService, TurnoService turnoService) {
        this.escolaService = escolaService;
        this.turnoService = turnoService;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Turnos", "Ver Grade", "Configurar", "Cancelar Mudança", "Voltar");
            if (escolha == 3 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    verGrade();
                    break;
                case 1:
                    configurar();
                    break;
                case 2:
                    cancelarMudanca();
                    break;
            }
        }
    }

    private void verGrade() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            Turno turno = Tela.escolherDaLista("Escolha o turno.", escola.getModeloTurno().turnos());
            if (turno == null) {
                return;
            }

            List<HorarioAula> versoes = turnoService.versoes(escola.getCie(), turno);
            if (versoes.isEmpty()) {
                Tela.mostrar("Este turno ainda não tem grade de horários.");
                return;
            }

            String[] dias = { "", "Seg", "Ter", "Qua", "Qui", "Sex" };
            String texto = "Horário de envio: " + escola.getHorarioEnvio(turno) + "\n\n";
            for (HorarioAula h : versoes) {
                texto += "A partir de " + h.getDataVigencia()
                        + " | " + dias[h.getDiaSemana()]
                        + " | " + h.getOrdemAula() + "ª aula "
                        + h.getHoraInicio() + " - " + h.getHoraFim() + "\n";
            }
            Tela.mostrarLista("Grade de " + escola.getNomeEscola(), texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void configurar() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            Turno turno = Tela.escolherDaLista("Escolha o turno.", escola.getModeloTurno().turnos());
            if (turno == null) {
                return;
            }

            List<HorarioAula> versoes = turnoService.versoes(escola.getCie(), turno);

            LocalTime envio = Tela.pedirHora("Horário de envio do turno:", escola.getHorarioEnvio(turno));
            if (envio == null) {
                return;
            }

            String[] dias = { "", "Segunda", "Terça", "Quarta", "Quinta", "Sexta" };
            List<HorarioAula> grade = new ArrayList<>();
            for (int dia = 1; dia <= 5; dia++) {
                String linha = Tela.pedirTexto(
                        "Aulas de " + dias[dia] + " (ex.: 07:30-08:20, 08:20-09:10). Deixe vazio se não tiver aula:");
                if (linha == null) {
                    return;
                }
                grade.addAll(lerAulas(dia, linha));
            }

            LocalDate vigencia = null;
            if (!versoes.isEmpty() && !grade.isEmpty()) {
                String texto = Tela.pedirTexto("A partir de quando a grade nova vale? (dd/mm/aaaa)");
                if (texto == null) {
                    return;
                }
                try {
                    vigencia = LocalDate.parse(texto.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "Data inválida. Use o formato dd/mm/aaaa, por exemplo 23/11/2026.");
                }
            }

            boolean salvouGrade = turnoService.salvarTurno(escola.getCie(), turno, envio, grade, vigencia);
            Tela.mostrar(salvouGrade ? "Turno salvo." : "Horário de envio salvo. O turno ainda não tem grade.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void cancelarMudanca() {
        try {
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            Turno turno = Tela.escolherDaLista("Escolha o turno.", escola.getModeloTurno().turnos());
            if (turno == null) {
                return;
            }

            List<HorarioAula> versoes = turnoService.versoes(escola.getCie(), turno);

            if (versoes.isEmpty()) {
                Tela.mostrar("Este turno não tem mudanças para cancelar.");
                return;
            }

            Set<LocalDate> datas = new LinkedHashSet<>();
            for (HorarioAula h : versoes) {
                datas.add(h.getDataVigencia());
            }

            LocalDate vigencia = Tela.escolherDaLista("Qual mudança cancelar?", new ArrayList<>(datas));
            if (vigencia == null) {
                return;
            }

            int resposta = Tela.escolher("Cancelar a grade que vale a partir de " + vigencia + "?", "Sim", "Não");
            if (resposta != 0) {
                return;
            }

            turnoService.cancelarMudanca(escola.getCie(), turno, vigencia);
            Tela.mostrar("Mudança cancelada.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private List<HorarioAula> lerAulas(int dia, String linha) {
        List<HorarioAula> aulas = new ArrayList<>();
        if (linha == null || linha.trim().equals("")) {
            return aulas;
        }

        String[] pedacos = linha.split(",");
        for (String pedaco : pedacos) {
            String[] horas = pedaco.trim().split("-");
            if (horas.length != 2) {
                throw new IllegalArgumentException("Use o formato HH:mm-HH:mm.");
            }

            HorarioAula a = new HorarioAula();
            a.setDiaSemana(dia);
            try {
                a.setHoraInicio(LocalTime.parse(horas[0].trim()));
                a.setHoraFim(LocalTime.parse(horas[1].trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Horário inválido: " + pedaco.trim() + ". Use o formato HH:mm-HH:mm.");
            }
            aulas.add(a);
        }
        return aulas;
    }

    private Escola escolherEscola() {
        List<Escola> escolas = escolaService.listarAcessiveis();

        if (escolas.size() == 1) {
            return escolas.get(0);
        }
        return Tela.escolherDaLista("Escolha a escola", escolas);
    }
}
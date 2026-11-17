package br.com.eduacompanha.main;

import java.time.Clock;
import java.time.ZoneId;

import br.com.eduacompanha.dao.AnoLetivoDao;
import br.com.eduacompanha.dao.EscolaDao;
import br.com.eduacompanha.dao.HorarioAulaDao;
import br.com.eduacompanha.dao.TurmaDao;
import br.com.eduacompanha.dao.UsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.service.AcessoService;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.TurmaService;
import br.com.eduacompanha.service.TurnoService;
import br.com.eduacompanha.view.MenuEscola;
import br.com.eduacompanha.view.MenuPrincipal;
import br.com.eduacompanha.view.MenuTurma;
import br.com.eduacompanha.view.MenuTurno;
import br.com.eduacompanha.view.TelaLogin;

public class Main {
        public static void main(String[] args) {
                Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

                // services aq.
                TurmaService turmaService = new TurmaService(new EscolaDao(), new TurmaDao(), relogio);
                EscolaService escolaService = new EscolaService(new EscolaDao(), new TurmaDao(), new UsuarioDao(),
                                new HorarioAulaDao(), relogio);
                AcessoService acessoService = new AcessoService(new UsuarioDao(), relogio);
                TurnoService turnoService = new TurnoService(new HorarioAulaDao(), new EscolaDao(), new AnoLetivoDao(),
                                relogio);

                // telas aq.
                TelaLogin telaLogin = new TelaLogin(acessoService);
                MenuPrincipal menuPrincipal = new MenuPrincipal(acessoService,
                                new MenuEscola(escolaService),
                                new MenuTurma(escolaService, turmaService),
                                new MenuTurno(escolaService, turnoService));

                // login aq.
                while (true) {
                        Usuario usuario = telaLogin.abrir();
                        if (usuario == null) {
                                return;
                        }
                        menuPrincipal.abrir();
                }

        }
}
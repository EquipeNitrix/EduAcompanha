package br.com.eduacompanha.main;

import java.time.Clock;
import java.time.ZoneId;

import br.com.eduacompanha.dao.EscolaDao;
import br.com.eduacompanha.dao.HorarioAulaDao;
import br.com.eduacompanha.dao.TurmaDao;
import br.com.eduacompanha.dao.UsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.service.AcessoService;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.TurmaService;
import br.com.eduacompanha.view.MenuEscola;
import br.com.eduacompanha.view.MenuTurma;
import br.com.eduacompanha.view.TelaLogin;

public class Main {
    public static void main(String[] args) {
        Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

        TurmaService turmaService = new TurmaService(new EscolaDao(), new TurmaDao(), relogio);
        EscolaService escolaService = new EscolaService(new EscolaDao(), new TurmaDao(), new UsuarioDao(),
                new HorarioAulaDao(), relogio);

        AcessoService acessoService = new AcessoService(new UsuarioDao(), relogio);

        Usuario usuario = new TelaLogin(acessoService).abrir();
        if (usuario == null) {
            return;
        }

        MenuEscola menuEscola = new MenuEscola(escolaService);

        MenuTurma menuTurma = new MenuTurma(escolaService, turmaService);
        menuTurma.abrir();
    }
}
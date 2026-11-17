package br.com.eduacompanha.main;

import java.time.Clock;
import java.time.ZoneId;

import br.com.eduacompanha.dao.AcompanhamentoCasoDao;
import br.com.eduacompanha.dao.AlunoDao;
import br.com.eduacompanha.dao.CasoBuscaAtivaDao;
import br.com.eduacompanha.dao.EscolaDao;
import br.com.eduacompanha.dao.HorarioAulaDao;
import br.com.eduacompanha.dao.MatriculaDao;
import br.com.eduacompanha.dao.ResponsavelDao;
import br.com.eduacompanha.dao.TurmaDao;
import br.com.eduacompanha.dao.UsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.service.BuscaAtivaService;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.TurmaService;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.view.MenuBuscaAtiva;
import br.com.eduacompanha.view.MenuEscola;
import br.com.eduacompanha.view.MenuTurma;

public class Main {
    public static void main(String[] args) {
        Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

        TurmaService turmaService = new TurmaService(new EscolaDao(), new TurmaDao(), relogio);
        EscolaService escolaService = new EscolaService(new EscolaDao(), new TurmaDao(), new UsuarioDao(),
                new HorarioAulaDao(), relogio);
        BuscaAtivaService buscaAtivaService = new BuscaAtivaService(new CasoBuscaAtivaDao(),
                new AcompanhamentoCasoDao(), new AlunoDao(), new MatriculaDao(), new TurmaDao(), new ResponsavelDao(),
                relogio);

        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfil.ADMIN);
        SessaoUtil.entrar(usuario);

        MenuEscola menuEscola = new MenuEscola(escolaService);
        // menuEscola.abrir();

        MenuTurma menuTurma = new MenuTurma(escolaService, turmaService);
        // menuTurma.abrir();

        MenuBuscaAtiva menuBuscaAtiva = new MenuBuscaAtiva(buscaAtivaService);
        menuBuscaAtiva.abrir();
    }
}
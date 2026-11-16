package br.com.eduacompanha.main;

import java.time.Clock;
import java.time.ZoneId;

import br.com.eduacompanha.dao.EscolaDao;
import br.com.eduacompanha.dao.HorarioAulaDao;
import br.com.eduacompanha.dao.TurmaDao;
import br.com.eduacompanha.dao.UsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.view.MenuEscola;

public class Main {
    public static void main(String[] args) {
        Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

        EscolaService escolaService = new EscolaService(new EscolaDao(), new TurmaDao(), new UsuarioDao(),
                new HorarioAulaDao(), relogio);

        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfil.ADMIN);
        SessaoUtil.entrar(usuario);

        MenuEscola menuEscola = new MenuEscola(escolaService);
        menuEscola.abrir();
    }
}
package br.com.eduacompanha.view;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.service.AcessoService;
import br.com.eduacompanha.util.SessaoUtil;

public class MenuPrincipal {
    private AcessoService acessoService;
    private MenuEscola menuEscola;
    private MenuTurma menuTurma;
    private MenuTurno menuTurno;

    public MenuPrincipal(AcessoService acessoService, MenuEscola menuEscola, MenuTurma menuTurma,
            MenuTurno menuTurno) {
        this.acessoService = acessoService;
        this.menuEscola = menuEscola;
        this.menuTurma = menuTurma;
        this.menuTurno = menuTurno;
    }

    // fica aberto até clicar pra sair
    public void abrir() {
        Usuario usuario = SessaoUtil.getUsuario();

        // mudar botoes conforme o perfil
        List<String> opcoes = new ArrayList<>();
        if (usuario.getPerfil() == Perfil.ADMIN) {
            opcoes.add("Escolas");
            opcoes.add("Turmas");
        }
        // admin e secretaria
        opcoes.add("Turnos e envio");
        opcoes.add("Sair");

        while (true) {
            int escolha = Tela.escolher("Olá, " + usuario.getNome() + ". O que você quer fazer?",
                    opcoes.toArray(new String[0]));

            if (escolha == -1) {
                acessoService.sair();
                return ;
            }

            switch (opcoes.get(escolha)) {
                case "Escolas":
                    menuEscola.abrir();
                    break;
             case "Turmas":
                    menuTurma.abrir();
                    break;
                case "Turnos e envio":
                    menuTurno.abrir();
                    break;
                case "Sair":
                    acessoService.sair();
                    return;
            }
        }

    }
}

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
    private MenuUsuario menuUsuario;
    private MenuBuscaAtiva menuBuscaAtiva;
    private MenuChamada menuChamada;
    private MenuAlerta menuAlerta;
    private MenuNotificacao menuNotificacao;

    public MenuPrincipal(AcessoService acessoService, MenuEscola menuEscola, MenuTurma menuTurma,
            MenuTurno menuTurno, MenuUsuario menuUsuario, MenuBuscaAtiva menuBuscaAtiva,
            MenuChamada menuChamada, MenuAlerta menuAlerta, MenuNotificacao menuNotificacao) {
        this.acessoService = acessoService;
        this.menuEscola = menuEscola;
        this.menuTurma = menuTurma;
        this.menuTurno = menuTurno;
        this.menuUsuario = menuUsuario;
        this.menuBuscaAtiva = menuBuscaAtiva;
        this.menuChamada = menuChamada;
        this.menuAlerta = menuAlerta;
        this.menuNotificacao = menuNotificacao;
    }

    // fica aberto até clicar pra sair
    public void abrir() {
        Usuario usuario = SessaoUtil.getUsuario();

        // mudar botoes conforme o perfil
        List<String> opcoes = new ArrayList<>();
        if (usuario.getPerfil() == Perfil.ADMIN) {
            opcoes.add("Escolas");
            opcoes.add("Usuarios");
            opcoes.add("Turmas");
            opcoes.add("Turnos e envio");
            opcoes.add("Notificações");
            opcoes.add("Alertas");
        } else {
            opcoes.add("Chamada");
            opcoes.add("Turnos e envio");
            opcoes.add("Alertas");
            opcoes.add("Notificações");
            opcoes.add("Busca Ativa");
            opcoes.add("Sair");
        }

        while (true) {
            int escolha = Tela.escolher("Olá, " + usuario.getNome() + ". O que você quer fazer?",
                    opcoes.toArray(new String[0]));

            if (escolha == -1) {
                acessoService.sair();
                return;
            }

            switch (opcoes.get(escolha)) {
                case "Escolas":
                    menuEscola.abrir();
                    break;
                case "Usuarios":
                    menuUsuario.abrir();
                    break;
                case "Turmas":
                    menuTurma.abrir();
                    break;
                case "Turnos e envio":
                    menuTurno.abrir();
                    break;
                case "Busca Ativa":
                    menuBuscaAtiva.abrir();
                    break;
                case "Chamada":
                    menuChamada.abrir();
                    break;
                case "Alertas":
                    menuAlerta.abrir();
                    break;
                case "Notificações":
                    menuNotificacao.abrir();
                    break;
                case "Sair":
                    acessoService.sair();
                    return;
            }
        }

    }
}

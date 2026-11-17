package br.com.eduacompanha.view;

import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.service.AcessoService;

public class TelaLogin {
    private AcessoService acessoService;

    public TelaLogin(AcessoService acessoService) {
        this.acessoService = acessoService;
    }

    // pedir login e senha. devolve o usuario ou null se for cancelado.
    public Usuario abrir() {
        while (true) {
            String login = Tela.pedirTexto("Login: ");
            if (login == null) {
                return null; // se clicou pra sair
            }

            String senha = Tela.pedirSenha("Senha");
            if (senha == null) {
                return null;
            }

            try {
                Usuario usuario = acessoService.login(login, senha);
                Tela.mostrar("Bem vindo, " + usuario.getNome() + "!");
                return usuario;
            } catch (Exception e) {
                Tela.erro(e);
            }

        }
    }
}

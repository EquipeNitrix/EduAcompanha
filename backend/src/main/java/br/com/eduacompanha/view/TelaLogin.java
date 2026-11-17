package br.com.eduacompanha.view;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

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

            String senha = pedirSenha();
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

    // o showInputDialog mostra a senha na tela. O JPasswordField mostra bolinhas no
    // lugar das letras.
    private String pedirSenha() {
        JPasswordField campo = new JPasswordField(20);
        int botao = JOptionPane.showConfirmDialog(null, campo, "Senha", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (botao != JOptionPane.OK_OPTION) {
            return null;
        }
        // getPassword devolve char, não String; new String converte.
        return new String(campo.getPassword());
    }
}

package br.com.eduacompanha.util;

import br.com.eduacompanha.model.Usuario;

public class SessaoUtil {
    private static Usuario usuario;

    public static Usuario getUsuario() {
        return usuario;
    }

    public static void entrar(Usuario u) {
        usuario = u;
    }

    public static void sair() {
        usuario = null;
    }
}

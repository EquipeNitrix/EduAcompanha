package br.com.eduacompanha.util;

import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.service.AcessoNegadoException;

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

    public static void exigirAdmin() {
        if (usuario == null || usuario.getPerfil() != Perfil.ADMIN) {
            throw new AcessoNegadoException("Seu perfil não tem permissão para esta ação.");
        }
    }
}

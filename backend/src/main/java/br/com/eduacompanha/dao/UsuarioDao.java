package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;

import br.com.eduacompanha.model.Usuario;

public class UsuarioDao implements IUsuarioDao {
    private Connection conexao;

    @Override
    public ArrayList<Usuario> consultar() {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Usuario consultar(int idUsuario) {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Usuario cadastrar(Usuario usuario) {

        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Usuario usuario, int idUsuario) {

        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idUsuario) {

        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public Usuario consultarPorLogin(String login) {

        throw new UnsupportedOperationException("Unimplemented method 'consultarPorLogin'");
    }

    @Override
    public boolean atualizarTentativas(int idUsuario, int tentativas, LocalDateTime bloqueadoAte) {

        throw new UnsupportedOperationException("Unimplemented method 'atualizarTentativas'");
    }

}

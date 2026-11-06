package br.com.eduacompanha.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import br.com.eduacompanha.model.Usuario;

public interface IUsuarioDao {
    public ArrayList<Usuario> consultar();

    public Usuario consultar(int idUsuario);

    public Usuario cadastrar(Usuario usuario);

    public boolean alterar(Usuario usuario, int idUsuario);

    public boolean excluir(int idUsuario);

    public Usuario consultarPorLogin(String login);

    public boolean atualizarTentativas(int idUsuario, int tentativas, LocalDateTime bloqueadoAte );
}

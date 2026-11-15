package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.util.JdbcUtil;
import br.com.eduacompanha.model.enums.Perfil;

public class UsuarioDao implements IUsuarioDao {
    private Connection conexao;

    public UsuarioDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Usuario> consultar() {
        ArrayList<Usuario> lstUsuario = new ArrayList<>();
        String query = "SELECT * FROM USUARIO";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstUsuario.add(montar(resultado));
            }

            return lstUsuario;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Usuario consultar(int idUsuario) {
        String query = "SELECT * FROM USUARIO WHERE id_usuario = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idUsuario);
            resultado = consulta.executeQuery();

            if (resultado.next()) {
                return montar(resultado);
            }
            return null;

        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Usuario cadastrar(Usuario usuario) {
        
    }

    @Override
    public boolean alterar(Usuario usuario, int idUsuario) {

    }

    @Override
    public boolean excluir(int idUsuario) {

    }

    @Override
    public Usuario consultarPorLogin(String login) {

    }

    @Override
    public boolean atualizarTentativas(int idUsuario, int tentativas, LocalDateTime bloqueadoAte) {

    }

    private Usuario montar(ResultSet resultado) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(resultado.getInt("id_usuario"));
        u.setNome(resultado.getString("nome"));
        u.setLogin(resultado.getString("login"));
        u.setSenhaHash(resultado.getString("senhaHash"));
        u.setPerfil(Perfil.valueOf(resultado.getString("perfil")));
        u.setAtivo(resultado.getBoolean("ativo"));
        u.setTentativasFalhas(resultado.getInt("tentativasFalhas"));
        u.setBloqueadoAte(JdbcUtil.lerDataHora(resultado, "bloqueadoAte"));
        u.setCie(resultado.getString("cie"));
        return u;
    }

}

package br.com.eduacompanha.dao;

import java.sql.Statement;
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
        String query = "INSERT INTO USUARIO (nome, login, senha_hash, perfil, ativo, "
                + "tentativas_falhas, bloqueado_ate, CIE) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement c = conexao.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            c.setString(1, usuario.getNome());
            c.setString(2, usuario.getLogin());
            c.setString(3, usuario.getSenhaHash());
            c.setString(4, usuario.getPerfil().name()); // aqui o enum ele vira texto com .name()
            c.setBoolean(5, usuario.isAtivo());
            c.setInt(6, usuario.getTentativasFalhas());
            c.setObject(7, usuario.getBloqueadoAte());
            c.setString(8, usuario.getCie());
            c.executeUpdate();

            // aqui pega o id e coloca num objeto.
            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                usuario.setIdUsuario(chaves.getInt(1));
            }
            return usuario; // volta com id preenchido.

        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }

    }

    @Override
    public boolean alterar(Usuario usuario, int idUsuario) {
        String query = "UPDATE USUARIO SET nome = ?, login = ?, senha_hash = ?, perfil = ?, "
                + "ativo = ?, tentativas_falhas = ?, bloqueado_ate = ?, CIE = ? "
                + "WHERE id_usuario = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, usuario.getNome());
            c.setString(2, usuario.getLogin());
            c.setString(3, usuario.getSenhaHash());
            c.setString(4, usuario.getPerfil().name()); // aqui o enum ele vira texto com .name()
            c.setBoolean(5, usuario.isAtivo());
            c.setInt(6, usuario.getTentativasFalhas());
            c.setObject(7, usuario.getBloqueadoAte());
            c.setString(8, usuario.getCie());
            c.setInt(9, idUsuario);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idUsuario) {
        String query = "DELETE FROM USUARIO WHERE id_usuario = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idUsuario);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Usuario consultarPorLogin(String login) {
        String query = "SELECT * FROM USUARIO WHERE login = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, login);
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
    public boolean atualizarTentativas(int idUsuario, int tentativas, LocalDateTime bloqueadoAte) {
        String query = "UPDATE USUARIO SET tentativas_falhas = ?, bloqueado_ate = ? WHERE id_usuario = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, tentativas);
            c.setObject(2, bloqueadoAte);
            c.setInt(3, idUsuario);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private Usuario montar(ResultSet resultado) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(resultado.getInt("id_usuario"));
        u.setNome(resultado.getString("nome"));
        u.setLogin(resultado.getString("login"));
        u.setSenhaHash(resultado.getString("senha_hash"));
        u.setPerfil(Perfil.valueOf(resultado.getString("perfil")));
        u.setAtivo(resultado.getBoolean("ativo"));
        u.setTentativasFalhas(resultado.getInt("tentativa_falhas"));
        u.setBloqueadoAte(JdbcUtil.lerDataHora(resultado, "bloqueado_ate"));
        u.setCie(resultado.getString("cie"));
        return u;
    }

}

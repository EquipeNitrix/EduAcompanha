package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.EnvioNotificacao;
import br.com.eduacompanha.util.JdbcUtil;

public class EnvioNotificacaoDao implements IEnvioNotificacaoDao {
    private Connection conexao;

    public EnvioNotificacaoDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<EnvioNotificacao> consultar() {
        ArrayList<EnvioNotificacao> lstEnvio = new ArrayList<>();
        String query = "SELECT * FROM ENVIO_NOTIFICACAO ORDER BY id_envio";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstEnvio.add(montar(resultado));
            }
            return lstEnvio;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public EnvioNotificacao consultar(int idEnvio) {
        String query = "SELECT * FROM ENVIO_NOTIFICACAO WHERE id_envio = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idEnvio);
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
    public EnvioNotificacao cadastrar(EnvioNotificacao envio) {
        String query = "INSERT INTO ENVIO_NOTIFICACAO (id_notificacao, id_contato, posicao, telefone_enviado, "
                + "enviado_em, entregue) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setInt(1, envio.getIdNotificacao());
            c.setObject(2, envio.getIdContato()); // Integer: null grava NULL
            c.setInt(3, envio.getPosicao());
            c.setString(4, envio.getTelefoneEnviado());
            c.setObject(5, envio.getEnviadoEm());
            c.setBoolean(6, envio.isEntregue());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                envio.setIdEnvio(chaves.getInt(1));
            }
            return envio;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // usado no "Simular 24 h", que adianta o enviado_em
    @Override
    public boolean alterar(EnvioNotificacao envio, int idEnvio) {
        String query = "UPDATE ENVIO_NOTIFICACAO SET id_notificacao = ?, id_contato = ?, posicao = ?, "
                + "telefone_enviado = ?, enviado_em = ?, entregue = ? WHERE id_envio = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, envio.getIdNotificacao());
            c.setObject(2, envio.getIdContato());
            c.setInt(3, envio.getPosicao());
            c.setString(4, envio.getTelefoneEnviado());
            c.setObject(5, envio.getEnviadoEm());
            c.setBoolean(6, envio.isEntregue());
            c.setInt(7, idEnvio);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idEnvio) {
        String query = "DELETE FROM ENVIO_NOTIFICACAO WHERE id_envio = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idEnvio);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // todos os envios de uma notificação, em ordem 
    @Override
    public ArrayList<EnvioNotificacao> consultarPorNotificacao(int idNotificacao) {
        ArrayList<EnvioNotificacao> lstEnvio = new ArrayList<>();
        String query = "SELECT * FROM ENVIO_NOTIFICACAO WHERE id_notificacao = ? ORDER BY posicao";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idNotificacao);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstEnvio.add(montar(resultado));
            }
            return lstEnvio;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // só o último envio de maior posição
    @Override
    public EnvioNotificacao consultarUltimoEnvio(int idNotificacao) {
        String query = "SELECT * FROM ENVIO_NOTIFICACAO WHERE id_notificacao = ? ORDER BY posicao DESC LIMIT 1";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idNotificacao);
            resultado = consulta.executeQuery();
            if (resultado.next()) {
                return montar(resultado);
            }
            return null;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private EnvioNotificacao montar(ResultSet resultado) throws SQLException {
        EnvioNotificacao e = new EnvioNotificacao();
        e.setIdEnvio(resultado.getInt("id_envio"));
        e.setIdNotificacao(resultado.getInt("id_notificacao"));
        e.setIdContato(resultado.getObject("id_contato", Integer.class)); // null se o telefone foi apagado
        e.setPosicao(resultado.getInt("posicao"));
        e.setTelefoneEnviado(resultado.getString("telefone_enviado"));
        e.setEnviadoEm(JdbcUtil.lerDataHora(resultado, "enviado_em"));
        e.setEntregue(resultado.getBoolean("entregue"));
        return e;
    }
}

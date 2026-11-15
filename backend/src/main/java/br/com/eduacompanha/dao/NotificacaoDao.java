package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Notificacao;
import br.com.eduacompanha.model.enums.StatusNotificacao;
import br.com.eduacompanha.util.JdbcUtil;

public class NotificacaoDao implements INotificacaoDao {
    private Connection conexao;

    public NotificacaoDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Notificacao> consultar() {
        ArrayList<Notificacao> lstNotificacao = new ArrayList<>();
        String query = "SELECT * FROM NOTIFICACAO ORDER BY data_falta DESC, id_notificacao DESC";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstNotificacao.add(montar(resultado));
            }
            return lstNotificacao;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Notificacao consultar(int idNotificacao) {
        String query = "SELECT * FROM NOTIFICACAO WHERE id_notificacao = ?";
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

    @Override
    public Notificacao cadastrar(Notificacao notificacao) {
        String query = "INSERT INTO NOTIFICACAO (id_matricula, data_falta, mensagem, status, resposta, "
                + "respondida_em, encerrada_em, observacao) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setInt(1, notificacao.getIdMatricula());
            c.setObject(2, notificacao.getDataFalta());
            c.setString(3, notificacao.getMensagem());
            c.setString(4, notificacao.getStatus().name());
            c.setString(5, notificacao.getResposta());
            c.setObject(6, notificacao.getRespondidaEm());
            c.setObject(7, notificacao.getEncerradaEm());
            c.setString(8, notificacao.getObservacao());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                notificacao.setIdNotificacao(chaves.getInt(1));
            }
            return notificacao;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    
    @Override
    public boolean alterar(Notificacao notificacao, int idNotificacao) {
        String query = "UPDATE NOTIFICACAO SET id_matricula = ?, data_falta = ?, mensagem = ?, status = ?, "
                + "resposta = ?, respondida_em = ?, encerrada_em = ?, observacao = ? WHERE id_notificacao = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, notificacao.getIdMatricula());
            c.setObject(2, notificacao.getDataFalta());
            c.setString(3, notificacao.getMensagem());
            c.setString(4, notificacao.getStatus().name());
            c.setString(5, notificacao.getResposta());
            c.setObject(6, notificacao.getRespondidaEm());
            c.setObject(7, notificacao.getEncerradaEm());
            c.setString(8, notificacao.getObservacao());
            c.setInt(9, idNotificacao);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idNotificacao) {
        String query = "DELETE FROM NOTIFICACAO WHERE id_notificacao = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idNotificacao);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // notificações que ainda esperam resposta
    @Override
    public List<Notificacao> consultarAguardando() {
        List<Notificacao> lstNotificacao = new ArrayList<>();
        String query = "SELECT * FROM NOTIFICACAO WHERE status = 'AGUARDANDO' ORDER BY data_falta, id_notificacao";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstNotificacao.add(montar(resultado));
            }
            return lstNotificacao;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // a notificação da falta daquele dia. null se ainda não tem
    @Override
    public Notificacao consultarPorMatriculaData(int idMatricula, LocalDate data) {
        String query = "SELECT * FROM NOTIFICACAO WHERE id_matricula = ? AND data_falta = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idMatricula);
            consulta.setObject(2, data);
            resultado = consulta.executeQuery();
            if (resultado.next()) {
                return montar(resultado);
            }
            return null;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private Notificacao montar(ResultSet resultado) throws SQLException {
        Notificacao n = new Notificacao();
        n.setIdNotificacao(resultado.getInt("id_notificacao"));
        n.setIdMatricula(resultado.getInt("id_matricula"));
        n.setDataFalta(JdbcUtil.lerData(resultado, "data_falta"));
        n.setMensagem(resultado.getString("mensagem"));
        n.setStatus(StatusNotificacao.valueOf(resultado.getString("status")));
        n.setResposta(resultado.getString("resposta"));
        n.setRespondidaEm(JdbcUtil.lerDataHora(resultado, "respondida_em"));
        n.setEncerradaEm(JdbcUtil.lerDataHora(resultado, "encerrada_em"));
        n.setObservacao(resultado.getString("observacao"));
        return n;
    }
}

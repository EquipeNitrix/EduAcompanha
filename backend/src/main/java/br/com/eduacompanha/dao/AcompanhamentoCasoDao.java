package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.AcompanhamentoCaso;
import br.com.eduacompanha.model.enums.TipoRegistro;
import br.com.eduacompanha.util.JdbcUtil;

public class AcompanhamentoCasoDao implements IAcompanhamentoCasoDao {
    private Connection conexao;

    public AcompanhamentoCasoDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<AcompanhamentoCaso> consultar() {
        ArrayList<AcompanhamentoCaso> lstAcompanhamento = new ArrayList<>();
        String query = "SELECT * FROM ACOMPANHAMENTO_CASO ORDER BY id_caso, data_hora";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstAcompanhamento.add(montar(resultado));
            }
            return lstAcompanhamento;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public AcompanhamentoCaso consultar(int idAcompanhamento) {
        String query = "SELECT * FROM ACOMPANHAMENTO_CASO WHERE id_acompanhamento = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idAcompanhamento);
            resultado = consulta.executeQuery();
            if (resultado.next()) {
                return montar(resultado);
            }
            return null;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // TENTATIVA preenche canal e resultado; ACAO preenche tipo_acao. Os outros vão null
    @Override
    public AcompanhamentoCaso cadastrar(AcompanhamentoCaso acompanhamentoCaso) {
        String query = "INSERT INTO ACOMPANHAMENTO_CASO (tipo_registro, data_hora, descricao, canal, resultado, "
                + "tipo_acao, id_caso, id_responsavel, id_usuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, acompanhamentoCaso.getTipoRegistro().name());
            c.setObject(2, acompanhamentoCaso.getDataHora());
            c.setString(3, acompanhamentoCaso.getDescricao());
            c.setString(4, acompanhamentoCaso.getCanal());
            c.setString(5, acompanhamentoCaso.getResultado());
            c.setString(6, acompanhamentoCaso.getTipoAcao());
            c.setInt(7, acompanhamentoCaso.getIdCaso());
            c.setObject(8, acompanhamentoCaso.getIdResponsavel()); // Integer: null grava NULL
            c.setInt(9, acompanhamentoCaso.getIdUsuario());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                acompanhamentoCaso.setIdAcompanhamento(chaves.getInt(1));
            }
            return acompanhamentoCaso;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(AcompanhamentoCaso acompanhamentoCaso, int idAcompanhamento) {
        String query = "UPDATE ACOMPANHAMENTO_CASO SET tipo_registro = ?, data_hora = ?, descricao = ?, canal = ?, "
                + "resultado = ?, tipo_acao = ?, id_caso = ?, id_responsavel = ?, id_usuario = ? "
                + "WHERE id_acompanhamento = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, acompanhamentoCaso.getTipoRegistro().name());
            c.setObject(2, acompanhamentoCaso.getDataHora());
            c.setString(3, acompanhamentoCaso.getDescricao());
            c.setString(4, acompanhamentoCaso.getCanal());
            c.setString(5, acompanhamentoCaso.getResultado());
            c.setString(6, acompanhamentoCaso.getTipoAcao());
            c.setInt(7, acompanhamentoCaso.getIdCaso());
            c.setObject(8, acompanhamentoCaso.getIdResponsavel());
            c.setInt(9, acompanhamentoCaso.getIdUsuario());
            c.setInt(10, idAcompanhamento);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idAcompanhamento) {
        String query = "DELETE FROM ACOMPANHAMENTO_CASO WHERE id_acompanhamento = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idAcompanhamento);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }


    @Override
    public List<AcompanhamentoCaso> consultarPorCaso(int idCaso) {
        List<AcompanhamentoCaso> lstAcompanhamento = new ArrayList<>();
        String query = "SELECT * FROM ACOMPANHAMENTO_CASO WHERE id_caso = ? ORDER BY data_hora, id_acompanhamento";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idCaso);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstAcompanhamento.add(montar(resultado));
            }
            return lstAcompanhamento;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private AcompanhamentoCaso montar(ResultSet resultado) throws SQLException {
        AcompanhamentoCaso a = new AcompanhamentoCaso();
        a.setIdAcompanhamento(resultado.getInt("id_acompanhamento"));
        a.setTipoRegistro(TipoRegistro.valueOf(resultado.getString("tipo_registro")));
        a.setDataHora(JdbcUtil.lerDataHora(resultado, "data_hora"));
        a.setDescricao(resultado.getString("descricao"));
        a.setCanal(resultado.getString("canal"));
        a.setResultado(resultado.getString("resultado"));
        a.setTipoAcao(resultado.getString("tipo_acao"));
        a.setIdCaso(resultado.getInt("id_caso"));
        a.setIdResponsavel(resultado.getObject("id_responsavel", Integer.class)); // null se não souber quem atendeu
        a.setIdUsuario(resultado.getInt("id_usuario"));
        return a;
    }
}

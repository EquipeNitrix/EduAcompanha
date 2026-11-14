package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.AnoLetivo;
import br.com.eduacompanha.util.JdbcUtil;

public class AnoLetivoDao implements IAnoLetivoDao {
    private Connection conexao;

    public AnoLetivoDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<AnoLetivo> consultar() {
        ArrayList<AnoLetivo> lstAnoLetivo = new ArrayList<>();
        String query = "SELECT * FROM ANO_LETIVO";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstAnoLetivo.add(montar(resultado));
            }

            return lstAnoLetivo;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public AnoLetivo consultar(int ano) {
        String query = "SELECT * FROM ANO_LETIVO WHERE ano = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, ano);
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
    public AnoLetivo cadastrar(AnoLetivo anoLetivo) {
        String query = "INSERT INTO ANO_LETIVO (ano, data_inicio, data_fim) "
                + "VALUES(?, ?, ?)";

        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, anoLetivo.getAno());
            c.setObject(2, anoLetivo.getDataInicio());
            c.setObject(3, anoLetivo.getDataFim());
            c.executeUpdate();
            return anoLetivo;

        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }

    }

    @Override
    public boolean alterar(AnoLetivo anoLetivo, int ano) {
        String query = "UPDATE ANO_LETIVO SET data_inicio = ?, data_fim = ? WHERE ano = ? ";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setObject(1, anoLetivo.getDataInicio());
            c.setObject(2, anoLetivo.getDataFim());
            c.setInt(3, ano);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int ano) {
        String query = "DELETE FROM ANO_LETIVO WHERE ano = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, ano);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private AnoLetivo montar(ResultSet resultado) throws SQLException {
        AnoLetivo anoL = new AnoLetivo();
        anoL.setAno(resultado.getInt("ano"));
        anoL.setDataInicio(JdbcUtil.lerData(resultado, "data_inicio"));
        anoL.setDataFim(JdbcUtil.lerData(resultado, "data_fim"));
        return anoL;
    }

}

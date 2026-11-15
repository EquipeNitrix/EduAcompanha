package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Responsavel;

public class ResponsavelDao implements IResponsavelDao {
    private Connection conexao;

    public ResponsavelDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Responsavel> consultar() {
        ArrayList<Responsavel> lstResponsavel = new ArrayList<>();
        String query = "SELECT * FROM RESPONSAVEL ORDER BY nome_responsavel";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstResponsavel.add(montar(resultado));
            }
            return lstResponsavel;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Responsavel consultar(int idResponsavel) {
        String query = "SELECT * FROM RESPONSAVEL WHERE id_responsavel = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idResponsavel);
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
    public Responsavel cadastrar(Responsavel responsavel) {
        String query = "INSERT INTO RESPONSAVEL (nome_responsavel, parentesco, id_aluno) VALUES (?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, responsavel.getNomeResponsavel());
            c.setString(2, responsavel.getParentesco());
            c.setInt(3, responsavel.getIdAluno());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                responsavel.setIdResponsavel(chaves.getInt(1));
            }
            return responsavel;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(Responsavel responsavel, int idResponsavel) {
        String query = "UPDATE RESPONSAVEL SET nome_responsavel = ?, parentesco = ?, id_aluno = ? "
                + "WHERE id_responsavel = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, responsavel.getNomeResponsavel());
            c.setString(2, responsavel.getParentesco());
            c.setInt(3, responsavel.getIdAluno());
            c.setInt(4, idResponsavel);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idResponsavel) {
        String query = "DELETE FROM RESPONSAVEL WHERE id_responsavel = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idResponsavel);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // responsáveis de um aluno
    @Override
    public List<Responsavel> consultarPorAluno(int idAluno) {
        List<Responsavel> lstResponsavel = new ArrayList<>();
        String query = "SELECT * FROM RESPONSAVEL WHERE id_aluno = ? ORDER BY id_responsavel";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idAluno);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstResponsavel.add(montar(resultado));
            }
            return lstResponsavel;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private Responsavel montar(ResultSet resultado) throws SQLException {
        Responsavel r = new Responsavel();
        r.setIdResponsavel(resultado.getInt("id_responsavel"));
        r.setNomeResponsavel(resultado.getString("nome_responsavel"));
        r.setParentesco(resultado.getString("parentesco"));
        r.setIdAluno(resultado.getInt("id_aluno"));
        return r;
    }
}

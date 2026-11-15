package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.Turno;

public class TurmaDao implements ITurmaDao {
    private Connection conexao;

    public TurmaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Turma> consultar() {
        ArrayList<Turma> lstTurma = new ArrayList<>();
        String query = "SELECT * FROM TURMA ORDER BY ano_letivo, nome_turma";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstTurma.add(montar(resultado));
            }
            return lstTurma;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Turma consultar(int idTurma) {
        String query = "SELECT * FROM TURMA WHERE id_turma = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idTurma);
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
    public Turma cadastrar(Turma turma) {
        String query = "INSERT INTO TURMA (nome_turma, turno, ano_letivo, CIE) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, turma.getNomeTurma());
            c.setString(2, turma.getTurno().name()); // enum vira texto
            c.setInt(3, turma.getAnoLetivo());
            c.setString(4, turma.getCie());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                turma.setIdTurma(chaves.getInt(1));
            }
            return turma;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(Turma turma, int idTurma) {
        String query = "UPDATE TURMA SET nome_turma = ?, turno = ?, ano_letivo = ?, CIE = ? WHERE id_turma = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, turma.getNomeTurma());
            c.setString(2, turma.getTurno().name());
            c.setInt(3, turma.getAnoLetivo());
            c.setString(4, turma.getCie());
            c.setInt(5, idTurma);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idTurma) {
        String query = "DELETE FROM TURMA WHERE id_turma = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idTurma);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // turmas de uma escola num ano letivo
    @Override
    public List<Turma> consultarPorEscola(String cie, int anoLetivo) {
        List<Turma> lstTurma = new ArrayList<>();
        String query = "SELECT * FROM TURMA WHERE CIE = ? AND ano_letivo = ? ORDER BY nome_turma";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, cie);
            consulta.setInt(2, anoLetivo);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstTurma.add(montar(resultado));
            }
            return lstTurma;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // turmas de um turno da escola
    @Override
    public List<Turma> consultarPorEscolaTurno(String cie, Turno turno, int anoLetivo) {
        List<Turma> lstTurma = new ArrayList<>();
        String query = "SELECT * FROM TURMA WHERE CIE = ? AND turno = ? AND ano_letivo = ? ORDER BY nome_turma";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, cie);
            consulta.setString(2, turno.name());
            consulta.setInt(3, anoLetivo);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstTurma.add(montar(resultado));
            }
            return lstTurma;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private Turma montar(ResultSet resultado) throws SQLException {
        Turma t = new Turma();
        t.setIdTurma(resultado.getInt("id_turma"));
        t.setNomeTurma(resultado.getString("nome_turma"));
        t.setTurno(Turno.valueOf(resultado.getString("turno"))); // texto vira enum
        t.setAnoLetivo(resultado.getInt("ano_letivo"));
        t.setCie(resultado.getString("CIE"));
        return t;
    }
}

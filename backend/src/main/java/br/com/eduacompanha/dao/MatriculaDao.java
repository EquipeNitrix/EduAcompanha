package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.enums.StatusMatricula;
import br.com.eduacompanha.util.JdbcUtil;

public class MatriculaDao implements IMatriculaDao {
    private Connection conexao;

    public MatriculaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Matricula> consultar() {
        ArrayList<Matricula> lstMatricula = new ArrayList<>();
        String query = "SELECT * FROM MATRICULA ORDER BY id_matricula";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstMatricula.add(montar(resultado));
            }
            return lstMatricula;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Matricula consultar(int idMatricula) {
        String query = "SELECT * FROM MATRICULA WHERE id_matricula = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idMatricula);
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
    public Matricula cadastrar(Matricula matricula) {
        String query = "INSERT INTO MATRICULA (id_aluno, id_turma, data_inicio, data_fim, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setInt(1, matricula.getIdAluno());
            c.setInt(2, matricula.getIdTurma());
            c.setObject(3, matricula.getDataInicio());
            c.setObject(4, matricula.getDataFim());
            c.setString(5, matricula.getStatus().name());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                matricula.setIdMatricula(chaves.getInt(1));
            }
            return matricula;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(Matricula matricula, int idMatricula) {
        String query = "UPDATE MATRICULA SET id_aluno = ?, id_turma = ?, data_inicio = ?, data_fim = ?, status = ? "
                + "WHERE id_matricula = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, matricula.getIdAluno());
            c.setInt(2, matricula.getIdTurma());
            c.setObject(3, matricula.getDataInicio());
            c.setObject(4, matricula.getDataFim());
            c.setString(5, matricula.getStatus().name());
            c.setInt(6, idMatricula);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idMatricula) {
        String query = "DELETE FROM MATRICULA WHERE id_matricula = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idMatricula);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // a matrícula ATIVA do aluno naquele ano.
    // o ano letivo está na TURMA, por isso tem JOIN
    @Override
    public Matricula consultarAtivaDoAluno(int idAluno, int anoLetivo) {
        String query = "SELECT m.* FROM MATRICULA m "
                + "JOIN TURMA t ON t.id_turma = m.id_turma "
                + "WHERE m.id_aluno = ? AND t.ano_letivo = ? AND m.status = 'ATIVA'";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idAluno);
            consulta.setInt(2, anoLetivo);
            resultado = consulta.executeQuery();
            if (resultado.next()) {
                return montar(resultado);
            }
            return null;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // todas as matrículas do aluno
    @Override
    public ArrayList<Matricula> consultarPorAluno(int idAluno) {
        ArrayList<Matricula> lstMatricula = new ArrayList<>();
        String query = "SELECT * FROM MATRICULA WHERE id_aluno = ? ORDER BY data_inicio DESC, id_matricula DESC";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idAluno);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstMatricula.add(montar(resultado));
            }
            return lstMatricula;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // matrículas ativas da turma
    @Override
    public ArrayList<Matricula> consultarAtivasDaTurma(int idTurma) {
        ArrayList<Matricula> lstMatricula = new ArrayList<>();
        String query = "SELECT * FROM MATRICULA WHERE id_turma = ? AND status = 'ATIVA'";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idTurma);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstMatricula.add(montar(resultado));
            }
            return lstMatricula;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private Matricula montar(ResultSet resultado) throws SQLException {
        Matricula m = new Matricula();
        m.setIdMatricula(resultado.getInt("id_matricula"));
        m.setIdAluno(resultado.getInt("id_aluno"));
        m.setIdTurma(resultado.getInt("id_turma"));
        m.setDataInicio(JdbcUtil.lerData(resultado, "data_inicio"));
        m.setDataFim(JdbcUtil.lerData(resultado, "data_fim"));
        m.setStatus(StatusMatricula.valueOf(resultado.getString("status")));
        return m;
    }
}

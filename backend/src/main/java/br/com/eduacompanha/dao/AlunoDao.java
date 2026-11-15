package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.ContatoResponsavel;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Responsavel;

public class AlunoDao implements IAlunoDao {
    private Connection conexao;

    public AlunoDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Aluno> consultar() {
        ArrayList<Aluno> lstAluno = new ArrayList<>();
        String query = "SELECT * FROM ALUNO";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstAluno.add(montar(resultado));
            }

            return lstAluno;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }

    }

    @Override
    public Aluno consultar(int idAluno) {
        String query = "SELECT * FROM ALUNO WHERE id_aluno = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idAluno);
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
    public Aluno cadastrar(Aluno aluno) {
        String query = "INSERT INTO ALUNO (RA, nome_aluno) VALUES (?, ?)";

        try {
            PreparedStatement c = conexao.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            c.setString(1, aluno.getRa());
            c.setString(2, aluno.getNomeAluno());
            c.executeUpdate();

            // aqui pega o id e coloca num objeto.
            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                aluno.setIdAluno(chaves.getInt(1));
            }
            return aluno; // volta com id preenchido.

        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(Aluno aluno, int idAluno) {
        String query = "UPDATE ALUNO SET RA = ?, nome_aluno = ? WHERE id_aluno = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, aluno.getRa());
            c.setString(2, aluno.getNomeAluno());
            c.setInt(3, idAluno);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idAluno) {
        String query = "DELETE FROM ALUNO WHERE id_aluno = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idAluno);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Aluno consultarPorRa(String ra) {
        String query = "SELECT * FROM ALUNO WHERE RA = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, ra);
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
    public List<Aluno> consultarPorNome(String trecho) {
        List<Aluno> lstAluno = new ArrayList<>();
        String query = "SELECT * FROM ALUNO WHERE nome_aluno LIKE ? ORDER BY nome_aluno";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, "%" + trecho + "%");
            resultado = consulta.executeQuery();

            while (resultado.next()) {
                lstAluno.add(montar(resultado));
            }
            return lstAluno;

        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Aluno cadastrarCompleto(Aluno aluno, Matricula matricula, Responsavel responsavel,
            List<ContatoResponsavel> telefones) {
        try {

            conexao.setAutoCommit(false); // serve para desligar o salvamento auto. Nada vai ser gravado até o commit()

            PreparedStatement c = conexao.prepareStatement(
                    "INSERT INTO ALUNO (RA, nome_aluno) VALUES (?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, aluno.getRa());
            c.setString(2, aluno.getNomeAluno());
            c.executeUpdate();
            ResultSet chaves = c.getGeneratedKeys();
            chaves.next();
            aluno.setIdAluno(chaves.getInt(1));

            c = conexao.prepareStatement(
                    "INSERT INTO MATRICULA (id_aluno, id_turma, data_inicio, status) VALUES (?, ?, ?, 'ATIVA')",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            c.setInt(1, aluno.getIdAluno());
            c.setInt(2, matricula.getIdTurma());
            c.setObject(3, matricula.getDataInicio());
            c.executeUpdate();
            chaves = c.getGeneratedKeys();
            chaves.next();
            matricula.setIdMatricula(chaves.getInt(1));
            matricula.setIdAluno(aluno.getIdAluno());

            c = conexao.prepareStatement(
                    "INSERT INTO RESPONSAVEL (nome_responsavel, parentesco, id_aluno) VALUES (?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, responsavel.getNomeResponsavel());
            c.setString(2, responsavel.getParentesco());
            c.setInt(3, aluno.getIdAluno());
            c.executeUpdate();
            chaves = c.getGeneratedKeys();
            chaves.next();
            responsavel.setIdResponsavel(chaves.getInt(1));
            responsavel.setIdAluno(aluno.getIdAluno());

            c = conexao.prepareStatement(
                    "INSERT INTO CONTATO_RESPONSAVEL (telefone, posicao, id_responsavel) VALUES (?, ?, ?)");
            for (ContatoResponsavel tel : telefones) {
                c.setString(1, tel.getTelefone());
                c.setInt(2, tel.getPosicao());
                c.setInt(3, responsavel.getIdResponsavel());
                c.executeUpdate();
            }

            conexao.commit();
            return aluno;

        } catch (SQLException erro) {
            try {
                conexao.rollback(); // se der erro em algum passo, ele desfaz tudo.
            } catch (SQLException e) {
                // se nem o rollback nao funcionar, ai se senta e chora amigao kkkkkkk
            }
            throw new RuntimeException(erro.getMessage(), erro);
        } finally {
            try {
                conexao.setAutoCommit(true); // volta ao normal p/ os outros metodos.
            } catch (SQLException e) {
            }
        }

    }

    private Aluno montar(ResultSet resultado) throws SQLException {
        Aluno alun = new Aluno();
        alun.setIdAluno(resultado.getInt("id_aluno"));
        alun.setRa(resultado.getString("RA"));
        alun.setNomeAluno(resultado.getString("nome_aluno"));
        return alun;
    }

}

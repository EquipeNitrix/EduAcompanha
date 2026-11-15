package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.ContatoResponsavel;

public class ContatoResponsavelDao implements IContatoResponsavelDao {
    private Connection conexao;

    public ContatoResponsavelDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<ContatoResponsavel> consultar() {
        ArrayList<ContatoResponsavel> lstContato = new ArrayList<>();
        String query = "SELECT * FROM CONTATO_RESPONSAVEL ORDER BY id_contato";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstContato.add(montar(resultado));
            }
            return lstContato;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public ContatoResponsavel consultar(int idContato) {
        String query = "SELECT * FROM CONTATO_RESPONSAVEL WHERE id_contato = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idContato);
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
    public ContatoResponsavel cadastrar(ContatoResponsavel contatoResponsavel) {
        String query = "INSERT INTO CONTATO_RESPONSAVEL (telefone, posicao, id_responsavel) VALUES (?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, contatoResponsavel.getTelefone());
            c.setInt(2, contatoResponsavel.getPosicao());
            c.setInt(3, contatoResponsavel.getIdResponsavel());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                contatoResponsavel.setIdContato(chaves.getInt(1));
            }
            return contatoResponsavel;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(ContatoResponsavel contatoResponsavel, int idContato) {
        String query = "UPDATE CONTATO_RESPONSAVEL SET telefone = ?, posicao = ?, id_responsavel = ? "
                + "WHERE id_contato = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, contatoResponsavel.getTelefone());
            c.setInt(2, contatoResponsavel.getPosicao());
            c.setInt(3, contatoResponsavel.getIdResponsavel());
            c.setInt(4, idContato);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idContato) {
        String query = "DELETE FROM CONTATO_RESPONSAVEL WHERE id_contato = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idContato);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // a fila de telefones do aluno, em ordem 
    @Override
    public List<ContatoResponsavel> consultarPorAluno(int idAluno) {
        List<ContatoResponsavel> lstContato = new ArrayList<>();
        String query = "SELECT c.* FROM CONTATO_RESPONSAVEL c "
                + "JOIN RESPONSAVEL r ON r.id_responsavel = c.id_responsavel "
                + "WHERE r.id_aluno = ? ORDER BY c.posicao";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idAluno);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstContato.add(montar(resultado));
            }
            return lstContato;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // muda a posição de um telefone só 
    @Override
    public boolean alterarPosicao(int idContato, int posicao) {
        String query = "UPDATE CONTATO_RESPONSAVEL SET posicao = ? WHERE id_contato = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, posicao);
            c.setInt(2, idContato);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // troca dois telefones de lugar na fila (botões subir e descer)
    @Override
    public boolean trocarPosicoes(int idContatoA, int idContatoB) {
        ContatoResponsavel a = consultar(idContatoA);
        ContatoResponsavel b = consultar(idContatoB);
        if (a == null || b == null) {
            return false;
        }
        try {
            conexao.setAutoCommit(false);

            PreparedStatement c = conexao.prepareStatement(
                    "UPDATE CONTATO_RESPONSAVEL SET posicao = ? WHERE id_contato = ?");
            c.setInt(1, b.getPosicao()); // A recebe a posição de B
            c.setInt(2, idContatoA);
            c.executeUpdate();

            c.setInt(1, a.getPosicao()); // B recebe a posição de A
            c.setInt(2, idContatoB);
            c.executeUpdate();

            conexao.commit();
            return true;
        } catch (SQLException erro) {
            try {
                conexao.rollback();
            } catch (SQLException e) {
            }
            throw new RuntimeException(erro.getMessage(), erro);
        } finally {
            try {
                conexao.setAutoCommit(true);
            } catch (SQLException e) {
            }
        }
    }

    private ContatoResponsavel montar(ResultSet resultado) throws SQLException {
        ContatoResponsavel c = new ContatoResponsavel();
        c.setIdContato(resultado.getInt("id_contato"));
        c.setTelefone(resultado.getString("telefone"));
        c.setPosicao(resultado.getInt("posicao"));
        c.setIdResponsavel(resultado.getInt("id_responsavel"));
        return c;
    }
}

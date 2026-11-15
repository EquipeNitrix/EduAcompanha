package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.CasoBuscaAtiva;
import br.com.eduacompanha.model.enums.StatusCaso;
import br.com.eduacompanha.util.JdbcUtil;

public class CasoBuscaAtivaDao implements ICasoBuscaAtivaDao {
    private Connection conexao;

    public CasoBuscaAtivaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<CasoBuscaAtiva> consultar() {
        ArrayList<CasoBuscaAtiva> lstCaso = new ArrayList<>();
        String query = "SELECT * FROM CASO_BUSCA_ATIVA ORDER BY data_abertura DESC, id_caso DESC";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstCaso.add(montar(resultado));
            }
            return lstCaso;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public CasoBuscaAtiva consultar(int idCaso) {
        String query = "SELECT * FROM CASO_BUSCA_ATIVA WHERE id_caso = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idCaso);
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
    public CasoBuscaAtiva cadastrar(CasoBuscaAtiva casoBuscaAtiva) {
        String query = "INSERT INTO CASO_BUSCA_ATIVA (data_abertura, status, data_encerramento, id_aluno, CIE, "
                + "motivo, desfecho, observacao_final, id_usuario_abertura) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setObject(1, casoBuscaAtiva.getDataAbertura());
            c.setString(2, casoBuscaAtiva.getStatus().name());
            c.setObject(3, casoBuscaAtiva.getDataEncerramento()); // null enquanto está aberto
            c.setInt(4, casoBuscaAtiva.getIdAluno());
            c.setString(5, casoBuscaAtiva.getCie());
            c.setString(6, casoBuscaAtiva.getMotivo());
            c.setString(7, casoBuscaAtiva.getDesfecho());
            c.setString(8, casoBuscaAtiva.getObservacaoFinal());
            c.setInt(9, casoBuscaAtiva.getIdUsuarioAbertura());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                casoBuscaAtiva.setIdCaso(chaves.getInt(1));
            }
            return casoBuscaAtiva;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // também é usado para encerrar atraves do service que poe o status e chama o alterar
    @Override
    public boolean alterar(CasoBuscaAtiva casoBuscaAtiva, int idCaso) {
        String query = "UPDATE CASO_BUSCA_ATIVA SET data_abertura = ?, status = ?, data_encerramento = ?, "
                + "id_aluno = ?, CIE = ?, motivo = ?, desfecho = ?, observacao_final = ?, id_usuario_abertura = ? "
                + "WHERE id_caso = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setObject(1, casoBuscaAtiva.getDataAbertura());
            c.setString(2, casoBuscaAtiva.getStatus().name());
            c.setObject(3, casoBuscaAtiva.getDataEncerramento());
            c.setInt(4, casoBuscaAtiva.getIdAluno());
            c.setString(5, casoBuscaAtiva.getCie());
            c.setString(6, casoBuscaAtiva.getMotivo());
            c.setString(7, casoBuscaAtiva.getDesfecho());
            c.setString(8, casoBuscaAtiva.getObservacaoFinal());
            c.setInt(9, casoBuscaAtiva.getIdUsuarioAbertura());
            c.setInt(10, idCaso);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idCaso) {
        String query = "DELETE FROM CASO_BUSCA_ATIVA WHERE id_caso = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idCaso);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // caso aberto do aluno; null se não tem (só pode 1 aberto por aluno
    @Override
    public CasoBuscaAtiva consultarAbertoDoAluno(int idAluno) {
        String query = "SELECT * FROM CASO_BUSCA_ATIVA WHERE id_aluno = ? AND status = 'ABERTO'";
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

    // casos da escola para a lista da tela. status null = abertos e encerrados
    @Override
    public List<CasoBuscaAtiva> consultarPorEscola(String cie, StatusCaso status) {
        List<CasoBuscaAtiva> lstCaso = new ArrayList<>();
        String query;
        if (status == null) {
            query = "SELECT * FROM CASO_BUSCA_ATIVA WHERE CIE = ? ORDER BY data_abertura DESC, id_caso DESC";
        } else {
            query = "SELECT * FROM CASO_BUSCA_ATIVA WHERE CIE = ? AND status = ? ORDER BY data_abertura DESC, id_caso DESC";
        }
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, cie);
            if (status != null) {
                consulta.setString(2, status.name());
            }
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstCaso.add(montar(resultado));
            }
            return lstCaso;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private CasoBuscaAtiva montar(ResultSet resultado) throws SQLException {
        CasoBuscaAtiva c = new CasoBuscaAtiva();
        c.setIdCaso(resultado.getInt("id_caso"));
        c.setDataAbertura(JdbcUtil.lerData(resultado, "data_abertura"));
        c.setStatus(StatusCaso.valueOf(resultado.getString("status")));
        c.setDataEncerramento(JdbcUtil.lerData(resultado, "data_encerramento"));
        c.setIdAluno(resultado.getInt("id_aluno"));
        c.setCie(resultado.getString("CIE"));
        c.setMotivo(resultado.getString("motivo"));
        c.setDesfecho(resultado.getString("desfecho"));
        c.setObservacaoFinal(resultado.getString("observacao_final"));
        c.setIdUsuarioAbertura(resultado.getInt("id_usuario_abertura"));
        return c;
    }
}

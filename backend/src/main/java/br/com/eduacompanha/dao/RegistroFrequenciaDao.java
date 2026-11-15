package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.RegistroFrequencia;
import br.com.eduacompanha.model.enums.StatusRegistro;
import br.com.eduacompanha.model.enums.TipoOcorrencia;
import br.com.eduacompanha.util.JdbcUtil;

public class RegistroFrequenciaDao implements IRegistroFrequenciaDao {
    private Connection conexao;

    public RegistroFrequenciaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<RegistroFrequencia> consultar() {
        ArrayList<RegistroFrequencia> lstRegistro = new ArrayList<>();
        String query = "SELECT * FROM REGISTRO_FREQUENCIA ORDER BY data, id_registro_freq";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstRegistro.add(montar(resultado));
            }
            return lstRegistro;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public RegistroFrequencia consultar(int idRegistroFreq) {
        String query = "SELECT * FROM REGISTRO_FREQUENCIA WHERE id_registro_freq = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idRegistroFreq);
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
    public RegistroFrequencia cadastrar(RegistroFrequencia registroFrequencia) {
        String query = "INSERT INTO REGISTRO_FREQUENCIA (data, tipo_ocorrencia, id_matricula, registrado_em, "
                + "id_usuario_registro, status, motivo_cancelamento, cancelado_em, id_usuario_cancelamento, "
                + "hora_entrada, hora_saida, observacao) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setObject(1, registroFrequencia.getData());
            c.setString(2, registroFrequencia.getTipoOcorrencia().name());
            c.setInt(3, registroFrequencia.getIdMatricula());
            c.setObject(4, registroFrequencia.getRegistradoEm());
            c.setInt(5, registroFrequencia.getIdUsuarioRegistro());
            c.setString(6, registroFrequencia.getStatus().name());
            // os campos abaixo podem ser null: setObject grava NULL sozinho
            c.setString(7, registroFrequencia.getMotivoCancelamento());
            c.setObject(8, registroFrequencia.getCanceladoEm());
            c.setObject(9, registroFrequencia.getIdUsuarioCancelamento());
            c.setObject(10, registroFrequencia.getHoraEntrada());
            c.setObject(11, registroFrequencia.getHoraSaida());
            c.setString(12, registroFrequencia.getObservacao());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                registroFrequencia.setIdRegistroFreq(chaves.getInt(1));
            }
            return registroFrequencia;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }


    @Override
    public boolean alterar(RegistroFrequencia registroFrequencia, int idRegistroFreq) {
        String query = "UPDATE REGISTRO_FREQUENCIA SET data = ?, tipo_ocorrencia = ?, id_matricula = ?, "
                + "registrado_em = ?, id_usuario_registro = ?, status = ?, motivo_cancelamento = ?, "
                + "cancelado_em = ?, id_usuario_cancelamento = ?, hora_entrada = ?, hora_saida = ?, observacao = ? "
                + "WHERE id_registro_freq = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setObject(1, registroFrequencia.getData());
            c.setString(2, registroFrequencia.getTipoOcorrencia().name());
            c.setInt(3, registroFrequencia.getIdMatricula());
            c.setObject(4, registroFrequencia.getRegistradoEm());
            c.setInt(5, registroFrequencia.getIdUsuarioRegistro());
            c.setString(6, registroFrequencia.getStatus().name());
            c.setString(7, registroFrequencia.getMotivoCancelamento());
            c.setObject(8, registroFrequencia.getCanceladoEm());
            c.setObject(9, registroFrequencia.getIdUsuarioCancelamento());
            c.setObject(10, registroFrequencia.getHoraEntrada());
            c.setObject(11, registroFrequencia.getHoraSaida());
            c.setString(12, registroFrequencia.getObservacao());
            c.setInt(13, idRegistroFreq);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idRegistroFreq) {
        String query = "DELETE FROM REGISTRO_FREQUENCIA WHERE id_registro_freq = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idRegistroFreq);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // todos os registros da matrícula, ativos e cancelados
    @Override
    public List<RegistroFrequencia> consultarPorMatricula(int idMatricula) {
        List<RegistroFrequencia> lstRegistro = new ArrayList<>();
        String query = "SELECT * FROM REGISTRO_FREQUENCIA WHERE id_matricula = ? ORDER BY data, id_registro_freq";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idMatricula);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstRegistro.add(montar(resultado));
            }
            return lstRegistro;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // registros da matrícula num dia
    @Override
    public List<RegistroFrequencia> consultarDoDia(int idMatricula, LocalDate data) {
        List<RegistroFrequencia> lstRegistro = new ArrayList<>();
        String query = "SELECT * FROM REGISTRO_FREQUENCIA WHERE id_matricula = ? AND data = ? ORDER BY id_registro_freq";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idMatricula);
            consulta.setObject(2, data);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstRegistro.add(montar(resultado));
            }
            return lstRegistro;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // muda o status e grava motivo, quem e quando (como pede o CHECK)
    @Override
    public boolean cancelar(int idRegistro, String motivo, int idUsuario, LocalDateTime canceladoEm) {
        String query = "UPDATE REGISTRO_FREQUENCIA SET status = 'CANCELADO', motivo_cancelamento = ?, "
                + "cancelado_em = ?, id_usuario_cancelamento = ? WHERE id_registro_freq = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, motivo);
            c.setObject(2, canceladoEm);
            c.setInt(3, idUsuario);
            c.setInt(4, idRegistro);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private RegistroFrequencia montar(ResultSet resultado) throws SQLException {
        RegistroFrequencia r = new RegistroFrequencia();
        r.setIdRegistroFreq(resultado.getInt("id_registro_freq"));
        r.setData(JdbcUtil.lerData(resultado, "data"));
        r.setTipoOcorrencia(TipoOcorrencia.valueOf(resultado.getString("tipo_ocorrencia")));
        r.setIdMatricula(resultado.getInt("id_matricula"));
        r.setRegistradoEm(JdbcUtil.lerDataHora(resultado, "registrado_em"));
        r.setIdUsuarioRegistro(resultado.getInt("id_usuario_registro"));
        r.setStatus(StatusRegistro.valueOf(resultado.getString("status")));
        r.setMotivoCancelamento(resultado.getString("motivo_cancelamento"));
        r.setCanceladoEm(JdbcUtil.lerDataHora(resultado, "cancelado_em"));
        // o getInt devolveria 0 para NULL. com o getObject com Integer.class devolve null mesmo
        r.setIdUsuarioCancelamento(resultado.getObject("id_usuario_cancelamento", Integer.class));
        r.setHoraEntrada(JdbcUtil.lerHora(resultado, "hora_entrada"));
        r.setHoraSaida(JdbcUtil.lerHora(resultado, "hora_saida"));
        r.setObservacao(resultado.getString("observacao"));
        return r;
    }
}

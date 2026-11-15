package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.util.JdbcUtil;

public class HorarioAulaDao implements IHorarioAulaDao {
    private Connection conexao;

    public HorarioAulaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<HorarioAula> consultar() {
        ArrayList<HorarioAula> lstHorario = new ArrayList<>();
        String query = "SELECT * FROM HORARIO_AULA ORDER BY CIE, turno, data_vigencia, dia_semana, ordem_aula";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstHorario.add(montar(resultado));
            }
            return lstHorario;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public HorarioAula consultar(int idHorario) {
        String query = "SELECT * FROM HORARIO_AULA WHERE id_horario = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setInt(1, idHorario);
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
    public HorarioAula cadastrar(HorarioAula horarioAula) {
        String query = "INSERT INTO HORARIO_AULA (CIE, turno, dia_semana, ordem_aula, hora_inicio, hora_fim, data_vigencia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement c = conexao.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            c.setString(1, horarioAula.getCie());
            c.setString(2, horarioAula.getTurno().name());
            c.setInt(3, horarioAula.getDiaSemana());
            c.setInt(4, horarioAula.getOrdemAula());
            c.setObject(5, horarioAula.getHoraInicio());
            c.setObject(6, horarioAula.getHoraFim());
            c.setObject(7, horarioAula.getDataVigencia());
            c.executeUpdate();

            ResultSet chaves = c.getGeneratedKeys();
            if (chaves.next()) {
                horarioAula.setIdHorario(chaves.getInt(1));
            }
            return horarioAula;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean alterar(HorarioAula horarioAula, int idHorario) {
        String query = "UPDATE HORARIO_AULA SET CIE = ?, turno = ?, dia_semana = ?, ordem_aula = ?, "
                + "hora_inicio = ?, hora_fim = ?, data_vigencia = ? WHERE id_horario = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, horarioAula.getCie());
            c.setString(2, horarioAula.getTurno().name());
            c.setInt(3, horarioAula.getDiaSemana());
            c.setInt(4, horarioAula.getOrdemAula());
            c.setObject(5, horarioAula.getHoraInicio());
            c.setObject(6, horarioAula.getHoraFim());
            c.setObject(7, horarioAula.getDataVigencia());
            c.setInt(8, idHorario);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public boolean excluir(int idHorario) {
        String query = "DELETE FROM HORARIO_AULA WHERE id_horario = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setInt(1, idHorario);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public List<HorarioAula> consultarPorEscolaTurno(String cie, Turno turno) {
        List<HorarioAula> lstHorario = new ArrayList<>();
        String query = "SELECT * FROM HORARIO_AULA WHERE CIE = ? AND turno = ? "
                + "ORDER BY data_vigencia, dia_semana, ordem_aula";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, cie);
            consulta.setString(2, turno.name());
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstHorario.add(montar(resultado));
            }
            return lstHorario;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    // grava uma versão inteira da grade numa transação: ou entram todas as aulas, ou nenhuma.
    // se já existir uma versão com a mesma data (ainda não usada), ela é apagada antes
    @Override
    public void cadastrarVersao(String cie, Turno turno, LocalDate vigencia, List<HorarioAula> aulas) {
        try {
            conexao.setAutoCommit(false);

            PreparedStatement apagar = conexao.prepareStatement(
                    "DELETE FROM HORARIO_AULA WHERE CIE = ? AND turno = ? AND data_vigencia = ?");
            apagar.setString(1, cie);
            apagar.setString(2, turno.name());
            apagar.setObject(3, vigencia);
            apagar.executeUpdate();

            PreparedStatement c = conexao.prepareStatement(
                    "INSERT INTO HORARIO_AULA (CIE, turno, dia_semana, ordem_aula, hora_inicio, hora_fim, data_vigencia) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)");
            for (HorarioAula aula : aulas) {
                c.setString(1, cie);
                c.setString(2, turno.name());
                c.setInt(3, aula.getDiaSemana());
                c.setInt(4, aula.getOrdemAula());
                c.setObject(5, aula.getHoraInicio());
                c.setObject(6, aula.getHoraFim());
                c.setObject(7, vigencia);
                c.executeUpdate();
            }

            conexao.commit();
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

    // apaga uma versão inteira (usado para cancelar uma mudança com data futura)
    @Override
    public boolean excluirVersao(String cie, Turno turno, LocalDate vigencia) {
        String query = "DELETE FROM HORARIO_AULA WHERE CIE = ? AND turno = ? AND data_vigencia = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, cie);
            c.setString(2, turno.name());
            c.setObject(3, vigencia);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    private HorarioAula montar(ResultSet resultado) throws SQLException {
        HorarioAula h = new HorarioAula();
        h.setIdHorario(resultado.getInt("id_horario"));
        h.setCie(resultado.getString("CIE"));
        h.setTurno(Turno.valueOf(resultado.getString("turno")));
        h.setDiaSemana(resultado.getInt("dia_semana"));
        h.setOrdemAula(resultado.getInt("ordem_aula"));
        h.setHoraInicio(JdbcUtil.lerHora(resultado, "hora_inicio"));
        h.setHoraFim(JdbcUtil.lerHora(resultado, "hora_fim"));
        h.setDataVigencia(JdbcUtil.lerData(resultado, "data_vigencia"));
        return h;
    }
}

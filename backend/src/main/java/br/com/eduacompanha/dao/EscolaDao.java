package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.enums.ModeloTurno;
import br.com.eduacompanha.util.JdbcUtil;

public class EscolaDao implements IEscolaDao {
    private Connection conexao;

    public EscolaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Escola> consultar() {
        ArrayList<Escola> lstEscolas = new ArrayList<>();
        String query = "SELECT * FROM escola";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            resultado = consulta.executeQuery();
            while (resultado.next()) {
                lstEscolas.add(montar(resultado));
            }

            return lstEscolas;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage(), erro);
        }
    }

    @Override
    public Escola consultar(String cie) {
        String query = "SELECT * FROM escola WHERE CIE = ?";
        PreparedStatement consulta = null;
        ResultSet resultado = null;
        try {
            consulta = conexao.prepareStatement(query);
            consulta.setString(1, cie);
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
    public Escola cadastrar(Escola escola) {
        String query = "INSERT INTO escola (CIE, nome_escola, municipio, modelo_turno, horario_envio_manha,\n"
                + "horario_envio_tarde, horario_envio_noite, horario_envio_integral)\n"
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?";

        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, escola.getCie());
            c.setString(2, escola.getNomeEscola());
            c.setString(3, escola.getMunicipio());
            c.setString(4, escola.getModeloTurno().name());
            // setObject aceita LocalTime e grava NULL quando o horário é null
            c.setObject(5, escola.getHorarioEnvioManha());
            c.setObject(6, escola.getHorarioEnvioTarde());
            c.setObject(7, escola.getHorarioEnvioNoite());
            c.setObject(8, escola.getHorarioEnvioIntegral());
            c.executeUpdate();
            return escola;

        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage());
        }
    }

    @Override
    public boolean alterar(Escola escola, String cie) {
        String query = "UPDATE escola SET nome_escola = ?, municipio = ?, modelo_turno = ?, "
                + "horario_envio_manha = ?, horario_envio_tarde = ?, horario_envio_noite = ?, "
                + "horario_envio_integral = ? WHERE CIE = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, escola.getNomeEscola());
            c.setString(2, escola.getMunicipio());
            c.setString(3, escola.getModeloTurno().name());
            c.setObject(4, escola.getHorarioEnvioManha());
            c.setObject(5, escola.getHorarioEnvioTarde());
            c.setObject(6, escola.getHorarioEnvioNoite());
            c.setObject(7, escola.getHorarioEnvioIntegral());
            c.setString(8, cie);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage());
        }
    }

    @Override
    public boolean excluir(String cie) {
        String query = "DELETE FROM escola WHERE CIE = ?";
        try {
            PreparedStatement c = conexao.prepareStatement(query);
            c.setString(1, cie);
            return c.executeUpdate() > 0;
        } catch (SQLException erro) {
            throw new RuntimeException(erro.getMessage());
        }
    }

    private Escola montar(ResultSet resultado) throws SQLException {
        Escola e = new Escola();
        e.setCie(resultado.getString("CIE"));
        e.setNomeEscola(resultado.getString("nome_escola"));
        e.setMunicipio(resultado.getString("municipio"));
        // texto do banco ("PARCIAL") vira enum
        e.setModeloTurno(ModeloTurno.valueOf(resultado.getString("modelo_turno")));
        // horários podem ser NULL
        e.setHorarioEnvioManha(JdbcUtil.lerHora(resultado, "horario_envio_manha"));
        e.setHorarioEnvioTarde(JdbcUtil.lerHora(resultado, "horario_envio_tarde"));
        e.setHorarioEnvioNoite(JdbcUtil.lerHora(resultado, "horario_envio_noite"));
        e.setHorarioEnvioIntegral(JdbcUtil.lerHora(resultado, "horario_envio_integral"));
        return e;
    }
}
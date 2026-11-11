package br.com.eduacompanha.util;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// métodos de apoio para os DAOs ler colunas que podem ser NULL.
// tudo é static
public class JdbcUtil {

    // ninguém precisa criar um objeto JdbcUtil
    private JdbcUtil() {
    }

    // (TIME): horários de envio, hora_entrada, hora_saida...
    public static LocalTime lerHora(ResultSet rs, String coluna) throws SQLException {
        Time t = rs.getTime(coluna);
        return t == null ? null : t.toLocalTime();
    }

    // grava uma hora no ?; se for null, grava NULL no banco
    public static void gravarHora(PreparedStatement ps, int posicao, LocalTime hora) throws SQLException {
        if (hora == null) {
            ps.setNull(posicao, Types.TIME);
        } else {
            ps.setTime(posicao, Time.valueOf(hora));
        }
    }

    // (DATE): data_fim da matrícula, data_encerramento do caso
    public static LocalDate lerData(ResultSet rs, String coluna) throws SQLException {
        Date d = rs.getDate(coluna);
        return d == null ? null : d.toLocalDate();
    }

    // (DATETIME): bloqueado_ate, cancelado_em, respondida_em
    public static LocalDateTime lerDataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp ts = rs.getTimestamp(coluna);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
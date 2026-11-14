package br.com.eduacompanha.main;

import java.util.ArrayList;

import br.com.eduacompanha.dao.EscolaDao;
import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Escola;

public class Main {
    public static void main(String[] args) throws Exception {

        /*
         * var conn = new ConnectionFactory().getConnection();
         * System.out.println("Conectou!");
         * 
         * var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM ESCOLA");
         * rs.next();
         * System.out.println("Escolas no banco: " + rs.getInt(1));
         * 
         * conn.close();
         * }
         */

        EscolaDao dao = new EscolaDao();

        ArrayList<Escola> escolas = dao.consultar();
        System.out.println("Escolas pelo DAO: " + escolas.size());

        for (Escola e : escolas) {
            System.out.println(e.getCie() + " - " + e.getNomeEscola()
                    + " - " + e.getModeloTurno()
                    + " - envio manhã: " + e.getHorarioEnvioManha());
        }

        // consultar por CIE
        Escola roque = dao.consultar("035262");
        System.out.println("Achou pelo CIE: " + (roque != null ? roque.getNomeEscola() : "não achou"));
        System.out.println("CIE que não existe: " + dao.consultar("999999")); // tem que dar null

    }
}

package br.com.eduacompanha.main;

import br.com.eduacompanha.factory.ConnectionFactory;

public class Main {
    public static void main(String[] args) throws Exception {
        var conn = new ConnectionFactory().getConnection();
        System.out.println("Conectou!");

        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM ESCOLA");
        rs.next();
        System.out.println("Escolas no banco: " + rs.getInt(1));

        conn.close();
    }
}

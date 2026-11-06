package br.com.eduacompanha.factory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class ConnectionFactory {

    public Connection getConnection() {
        try (InputStream arquivo = ConnectionFactory.class.getResourceAsStream("/db.properties")) {

            if (arquivo == null) {
                throw new RuntimeException(
                        "Arquivo db.properties não encontrado. Copie o db.properties.exemplo e coloque sua senha.");
            }

            Properties config = new Properties();
            config.load(arquivo);

            return DriverManager.getConnection(
                    config.getProperty("db.url"),
                    config.getProperty("db.usuario"),
                    config.getProperty("db.senha"));

        } catch (Exception erro) {
            throw new RuntimeException(erro);
        }
    }
}

package br.com.eduacompanha.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SenhaUtil {

    public static String gerarHash(String senha) {
        try {
            // pega a maquina que calcula o SHA-256 do MYSQL
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            // o messageDigest nao aceita texto, só bytes
            // o getbytes converte a senha em bytes
            byte[] byteDaSenha = senha.getBytes(StandardCharsets.UTF_8);

            // calcula o hash: 32 bytes
            byte[] hash = sha.digest(byteDaSenha);

            // o banco guarda o hash como hexadecimal
            // cada byte vira dois caracter ( %02x )

            StringBuilder texto = new StringBuilder();
            for (byte b : hash) {
                texto.append(String.format("%02x", b));
            }
            return texto.toString();

        } catch (NoSuchAlgorithmException erro) {
            // só acontece se o Java não tiver SHA-256, e todo Java tem.
            // mesmo assim o Java obriga a tratar, então vira RuntimeException
            throw new RuntimeException("SHA-256 não disponível", erro);
        }
    }
}

package br.com.eduacompanha.util;

public class TextoUtil {

    public static String limparEspacos(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim().replaceAll("\\s+", " ");
        if (limpo.equals("")) {
            return null;
        }
        return limpo;
    }
}

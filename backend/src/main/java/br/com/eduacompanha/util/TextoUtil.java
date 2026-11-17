package br.com.eduacompanha.util;

import java.text.Normalizer;

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

    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
        return normalizado;
    }
}

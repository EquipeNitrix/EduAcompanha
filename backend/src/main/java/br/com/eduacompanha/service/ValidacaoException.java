package br.com.eduacompanha.service;

import java.util.Map;

public class ValidacaoException extends RuntimeException {
    private Map<String, String> campos;

    public ValidacaoException(Map<String, String> campos) {
        super("Há campos para corrigir.");
        this.campos = campos;
    }

    public ValidacaoException(String campo, String mensagem) {
        this(Map.of(campo, mensagem));
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}

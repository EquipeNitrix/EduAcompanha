package br.com.eduacompanha.model.enums;

public enum Perfil {

    ADMIN("admin"),
    SECRETARIA("secretaria");

    private final String codigoApi;

    // construtor
    Perfil(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static Perfil deCodigoApi(String codigo) {
        for (Perfil perfil : values()) {

            if (perfil.codigoApi.equals(codigo)) {
                return perfil;
            }
        }

        throw new IllegalArgumentException("Perfil inválido: " + codigo);
    }
}



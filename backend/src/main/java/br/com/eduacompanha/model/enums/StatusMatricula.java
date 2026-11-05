package br.com.eduacompanha.model.enums;

public enum StatusMatricula {
    ATIVA("ativa"),
    CANCELADA("cancelada");

    private final String codigoApi;

    // construtor
    StatusMatricula(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static StatusMatricula deCodigoApi(String codigo) {
        for (StatusMatricula statusMatricula : values()) {

            if (statusMatricula.codigoApi.equals(codigo)) {
                return statusMatricula;
            }
        }

        throw new IllegalArgumentException("Status da matricula inválido: " + codigo);
    }
}

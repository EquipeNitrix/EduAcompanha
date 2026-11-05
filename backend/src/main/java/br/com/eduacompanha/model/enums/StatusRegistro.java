package br.com.eduacompanha.model.enums;

public enum StatusRegistro {

    ATIVO("ativo"),
    CANCELADO("cancelado");

    private final String codigoApi;

    // construtor
    StatusRegistro(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static StatusRegistro deCodigoApi(String codigo) {
        for (StatusRegistro statusRegistro : values()) {

            if (statusRegistro.codigoApi.equals(codigo)) {
                return statusRegistro;
            }
        }

        throw new IllegalArgumentException("Status do registro inválido: " + codigo);
    }
}


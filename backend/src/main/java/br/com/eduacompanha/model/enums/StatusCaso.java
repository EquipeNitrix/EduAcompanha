package br.com.eduacompanha.model.enums;

public enum StatusCaso {
    ABERTO("aberto"),
    ENCERRADO("encerrado");

    private final String codigoApi;

    // construtor
    StatusCaso(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static StatusCaso deCodigoApi(String codigo) {
        for (StatusCaso statusCaso : values()) {

            if (statusCaso.codigoApi.equals(codigo)) {
                return statusCaso;
            }
        }

        throw new IllegalArgumentException("Status do caso inválido: " + codigo);
    }
}

package br.com.eduacompanha.model.enums;

public enum TipoRegistro {
    TENTATIVA("tentativa"),
    ACAO("acao");

    private final String codigoApi;

    // construtor
    TipoRegistro(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static TipoRegistro deCodigoApi(String codigo) {
        for (TipoRegistro tipoRegistro : values()) {

            if (tipoRegistro.codigoApi.equals(codigo)) {
                return tipoRegistro;
            }
        }

        throw new IllegalArgumentException("Tipo de registro inválido: " + codigo);
    }
}

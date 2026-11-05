package br.com.eduacompanha.model.enums;

public enum Turno {

    MANHA("manha"),
    TARDE("tarde"),
    NOITE("noite"),
    INTEGRAL("integral");

    private final String codigoApi;

    // construtor
    Turno(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static Turno deCodigoApi(String codigo) {
        for (Turno turno : values()) {

            if (turno.codigoApi.equals(codigo)) {
                return turno;
            }
        }

        throw new IllegalArgumentException("Turno inválido: " + codigo);
    }
}

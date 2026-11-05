package br.com.eduacompanha.model.enums;

public enum ModeloTurno {

    PARCIAL("parcial"),
    INTEGRAL("integral");

    private final String codigoApi;

    // construtor
    ModeloTurno(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static ModeloTurno deCodigoApi(String codigo) {
        for (ModeloTurno modelo : values()) {

            if (modelo.codigoApi.equals(codigo)) {
                return modelo;
            }
        }

        throw new IllegalArgumentException("Modelo de turno inválido: " + codigo);
    }
}

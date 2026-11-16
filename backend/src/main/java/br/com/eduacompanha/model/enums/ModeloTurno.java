package br.com.eduacompanha.model.enums;

import java.util.List;

public enum ModeloTurno {

    PARCIAL("parcial", "Manhã, tarde e noite"),
    INTEGRAL("integral", "Integral");

    private final String codigoApi;
    private final String nome;

    // construtor
    ModeloTurno(String codigoApi, String nome) {
        this.codigoApi = codigoApi;
        this.nome = nome;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public String getNome() {
        return nome;
    }

    public static ModeloTurno deCodigoApi(String codigo) {
        for (ModeloTurno modelo : values()) {

            if (modelo.codigoApi.equals(codigo)) {
                return modelo;
            }
        }

        throw new IllegalArgumentException("Modelo de turno inválido: " + codigo);
    }

    public List<Turno> turnos() {
        if (this == ModeloTurno.INTEGRAL) {
            return List.of(Turno.INTEGRAL);
        } else {
            return List.of(Turno.MANHA, Turno.TARDE, Turno.NOITE);
        }
    }
}

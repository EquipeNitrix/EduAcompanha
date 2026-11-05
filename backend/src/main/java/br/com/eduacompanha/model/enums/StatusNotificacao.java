package br.com.eduacompanha.model.enums;

public enum StatusNotificacao {
    AGUARDANDO("aguardando"),
    RESPONDIDA("respondida"),
    SEM_RESPOSTA("sem resposta"),
    INTERROMPIDA("interrompida");

    private final String codigoApi;

    // construtor
    StatusNotificacao(String codigoApi) {
        this.codigoApi = codigoApi;
    }

    public String getCodigoApi() {
        return codigoApi;
    }

    public static StatusNotificacao deCodigoApi(String codigo) {
        for (StatusNotificacao statusNotificacao : values()) {

            if (statusNotificacao.codigoApi.equals(codigo)) {
                return statusNotificacao;
            }
        }

        throw new IllegalArgumentException("Status de notificação inválido: " + codigo);
    }
}

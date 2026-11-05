package br.com.eduacompanha.model.enums;

public enum TipoOcorrencia {

    // o que o site manda e espera receber
    // Cria a constante e escolhe o código dela
    FALTA("falta"),
    ATRASO("atraso"),
    SAIDA_ANTECIPADA("saida"),
    SAIDA_COM_RETORNO("saidaRetorno");

    // cada constante acima tem seu proprio CodigoApi, igual cada objeto aluno tem
    // seu proprio nome
    // o "final" diz: essa gaveta só pode ser preenchida UMA vez
    // Declara o atributo que guarda o código
    private final String codigoApi;

    // construtor
    TipoOcorrencia(String codigoApi) {
        this.codigoApi = codigoApi; // aqui a gaveta fica preenchida
    }

    // getter, quando a API manda uma ocorrencia pro site, ela precisa escrever por exemplo:
    // "saida" no JSON e não "SAIDA_ANTECIPADA", ai usa
    //SAIDA_ANTECIPADA.getCodigoApi() -> "saída"
    public String getCodigoApi() {
        return codigoApi;
    }

    // values() devolve as 4 constantes: FALTA, ATRASO, SAIDA_ANTECIPADA,
    // SAIDA_COM_RETORNO.
    // O for olha uma por uma.
    public static TipoOcorrencia deCodigoApi(String codigo){
        for (TipoOcorrencia tipo : values()){
            // Compara o apelido guardado nesta constante com o texto
            // que veio do site. Se for igual, achou.
            if (tipo.codigoApi.equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de ocorrência inválido: " + codigo);
    }
}
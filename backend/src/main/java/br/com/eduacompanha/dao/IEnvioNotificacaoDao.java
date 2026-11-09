package br.com.eduacompanha.dao;

import java.util.ArrayList;

public interface IEnvioNotificacaoDao {
    public ArrayList<EnvioNotificacao> consultar();

    public EnvioNotificacao consultar(int idEnvio);

    public EnvioNotificacao cadastrar(EnvioNotificacao envio);

    public boolean alterar(EnvioNotificacao envio, int idEnvio);

    public boolean excluir(int idEnvio);
    
    // usa na tela de detalhe da notificação
    public ArrayList<EnvioNotificacao> consultarPorNotificacao(int idNotificacao);

    // só o último envio. o Service vai ver o enviadoEm para saber se já passaram 24 h
    // e o entregue para saber se falhou. null se ainda não houve envio
    public EnvioNotificacao consultarUltimoEnvio(int idNotificacao);
}

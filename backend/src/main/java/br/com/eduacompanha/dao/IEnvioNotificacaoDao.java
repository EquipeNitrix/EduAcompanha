package br.com.eduacompanha.dao;

import java.util.ArrayList;

public interface IEnvioNotificacaoDao {
    public ArrayList<EnvioNotificacaoDao> consultar();

    public EnvioNotificacaoDao consultar(int idEnvio);

    public EnvioNotificacaoDao cadastrar(EnvioNotificacaoDao envio);

    public boolean alterar(EnvioNotificacaoDao envio, int idEnvio);

    public boolean excluir(int idEnvio);
    
    // usa na tela de detalhe da notificação
    public ArrayList<EnvioNotificacaoDao> consultarPorNotificacao(int idNotificacao);

    // só o último envio. o Service vai ver o enviadoEm para saber se já passaram 24 h
    // e o entregue para saber se falhou. null se ainda não houve envio
    public EnvioNotificacaoDao consultarUltimoEnvio(int idNotificacao);
}

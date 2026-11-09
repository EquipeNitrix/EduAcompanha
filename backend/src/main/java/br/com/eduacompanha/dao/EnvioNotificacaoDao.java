package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;

import br.com.eduacompanha.factory.ConnectionFactory;

public class EnvioNotificacaoDao implements IEnvioNotificacaoDao {
    private Connection conexao;
    public EnvioNotificacaoDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<EnvioNotificacaoDao> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public EnvioNotificacaoDao consultar(int idEnvio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public EnvioNotificacaoDao cadastrar(EnvioNotificacaoDao envio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(EnvioNotificacaoDao envio, int idEnvio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idEnvio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public ArrayList<EnvioNotificacaoDao> consultarPorNotificacao(int idNotificacao) {
      
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorNotificacao'");
    }

    @Override
    public EnvioNotificacaoDao consultarUltimoEnvio(int idNotificacao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarUltimoEnvio'");
    }
    
}

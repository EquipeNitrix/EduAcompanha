package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import br.com.eduacompanha.model.EnvioNotificacao;
import br.com.eduacompanha.factory.ConnectionFactory;

public class EnvioNotificacaoDao implements IEnvioNotificacaoDao {
    private Connection conexao;
    public EnvioNotificacaoDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<EnvioNotificacao> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public EnvioNotificacao consultar(int idEnvio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public EnvioNotificacao cadastrar(EnvioNotificacao envio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(EnvioNotificacao envio, int idEnvio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idEnvio) {
       
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public ArrayList<EnvioNotificacao> consultarPorNotificacao(int idNotificacao) {
      
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorNotificacao'");
    }

    @Override
    public EnvioNotificacao consultarUltimoEnvio(int idNotificacao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarUltimoEnvio'");
    }
    
}

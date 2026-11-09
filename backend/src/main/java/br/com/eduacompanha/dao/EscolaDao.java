package br.com.eduacompanha.dao;
import java.sql.Connection;
import java.util.ArrayList;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Escola;


public class EscolaDao implements IEscolaDao {
    private Connection conexao;
    public EscolaDao() {
        conexao = new ConnectionFactory().getConnection();
    }

    @Override
    public ArrayList<Escola> consultar() {
    

        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Escola consultar(String cie) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Escola cadastrar(Escola escola) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Escola escola, String cie) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(String cie) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }
}
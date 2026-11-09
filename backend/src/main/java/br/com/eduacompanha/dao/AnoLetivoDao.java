package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.AnoLetivo;

public class AnoLetivoDao implements IAnoLetivoDao {
    private Connection conexao;
    public AnoLetivoDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<AnoLetivo> consultar() {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public AnoLetivo consultar(int ano) {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public AnoLetivo cadastrar(AnoLetivo anoLetivo) {

        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(AnoLetivo anoLetivo, int ano) {

        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int ano) {

        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

}

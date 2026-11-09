package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.AcompanhamentoCaso;

public class AcompanhamentoCasoDao implements IAcompanhamentoCasoDao {
    private Connection conexao;
    public AcompanhamentoCasoDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<AcompanhamentoCaso> consultar() {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public AcompanhamentoCaso consultar(int idAcompanhamento) {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public AcompanhamentoCaso cadastrar(AcompanhamentoCaso acompanhamentoCaso) {

        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(AcompanhamentoCaso acompanhamentoCaso, int idAcompanhamento) {

        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idAcompanhamento) {

        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<AcompanhamentoCaso> consultarPorCaso(int idCaso) {

        throw new UnsupportedOperationException("Unimplemented method 'consultarPorCaso'");
    }

}

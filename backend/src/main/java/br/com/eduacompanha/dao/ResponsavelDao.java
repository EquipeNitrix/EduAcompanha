package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Responsavel;

public class ResponsavelDao implements IResponsavelDao {
    private Connection conexao;
    public ResponsavelDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<Responsavel> consultar() {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Responsavel consultar(int idResponsavel) {

        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Responsavel cadastrar(Responsavel responsavel) {

        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Responsavel responsavel, int idResponsavel) {

        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idResponsavel) {

        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<Responsavel> consultarPorAluno(int idAluno) {

        throw new UnsupportedOperationException("Unimplemented method 'consultarPorAluno'");
    }

}

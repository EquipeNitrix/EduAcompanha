package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.ContatoResponsavel;

public class ContatoResponsavelDao implements IContatoResponsavelDao{
    private Connection conexao;
    public ContatoResponsavelDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<ContatoResponsavel> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public ContatoResponsavel consultar(int idContato) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public ContatoResponsavel cadastrar(ContatoResponsavel contatoResponsavel) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(ContatoResponsavel contatoResponsavel, int idContato) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idContato) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<ContatoResponsavel> consultarPorAluno(int idAluno) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorAluno'");
    }

    @Override
    public boolean alterarPosicao(int idContato, int posicao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterarPosicao'");
    }

    @Override
    public boolean trocarPosicoes(int idContatoA, int idContatoB) {
        
        throw new UnsupportedOperationException("Unimplemented method 'trocarPosicoes'");
    }
    
}

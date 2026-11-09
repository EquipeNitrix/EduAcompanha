package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.ContatoResponsavel;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Responsavel;

public class AlunoDao implements IAlunoDao {
    private Connection conexao;

    @Override
    public ArrayList<Aluno> consultar() {
       
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Aluno consultar(int idAluno) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Aluno cadastrar(Aluno aluno) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Aluno aluno, int idAluno) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idAluno) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public Aluno consultarPorRa(String ra) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorRa'");
    }

    @Override
    public List<Aluno> consultarPorNome(String trecho) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorNome'");
    }

    @Override
    public Aluno cadastrarCompleto(Aluno aluno, Matricula matricula, Responsavel responsavel,
            List<ContatoResponsavel> telefones) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrarCompleto'");
    }
    
}

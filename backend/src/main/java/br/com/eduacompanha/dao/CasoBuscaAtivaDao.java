package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.CasoBuscaAtiva;
import br.com.eduacompanha.model.enums.StatusCaso;

public class CasoBuscaAtivaDao implements ICasoBuscaAtivaDao {
    private Connection conexao;
    public CasoBuscaAtivaDao() {
    conexao = new ConnectionFactory().getConnection();

    }

    @Override
    public ArrayList<CasoBuscaAtiva> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public CasoBuscaAtiva consultar(int idCaso) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public CasoBuscaAtiva cadastrar(CasoBuscaAtiva casoBuscaAtiva) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(CasoBuscaAtiva casoBuscaAtiva, int idCaso) {
       
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idCaso) {
      
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public CasoBuscaAtiva consultarAbertoDoAluno(int idAluno) {
       
        throw new UnsupportedOperationException("Unimplemented method 'consultarAbertoDoAluno'");
    }

    @Override
    public List<CasoBuscaAtiva> consultarPorEscola(String cie, StatusCaso status) {
       
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorEscola'");
    }
    
}

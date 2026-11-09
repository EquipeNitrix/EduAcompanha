package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.Turno;

public class TurmaDao implements ITurmaDao{
    private Connection conexao;
    public TurmaDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<Turma> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Turma consultar(int idTurma) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Turma cadastrar(Turma turma) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Turma turma, int idTurma) {
       
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idTurma) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<Turma> consultarPorEscola(String cie, int anoLetivo) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorEscola'");
    }

    @Override
    public List<Turma> consultarPorEscolaTurno(String cie, Turno turno, int anoLetivo) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorEscolaTurno'");
    }
    
}

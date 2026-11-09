package br.com.eduacompanha.dao;

import java.util.ArrayList;

import br.com.eduacompanha.model.Matricula;

public class MatriculaDao implements IMatriculaDao {

    @Override
    public ArrayList<Matricula> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Matricula consultar(int idMatricula) {
      
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Matricula cadastrar(Matricula matricula) {
       
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Matricula matricula, int idMatricula) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idMatricula) {
     
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public Matricula consultarAtivaDoAluno(int idAluno, int anoLetivo) {
       
        throw new UnsupportedOperationException("Unimplemented method 'consultarAtivaDoAluno'");
    }

    @Override
    public ArrayList<Matricula> consultarPorAluno(int idAluno) {
      
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorAluno'");
    }

    @Override
    public ArrayList<Matricula> consultarAtivasDaTurma(int idTurma) {
     
        throw new UnsupportedOperationException("Unimplemented method 'consultarAtivasDaTurma'");
    }
    
}


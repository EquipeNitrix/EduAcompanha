package br.com.eduacompanha.dao;

import java.util.ArrayList;

import br.com.eduacompanha.model.Matricula;

public interface IMatriculaDao {
    public ArrayList<Matricula> consultar();

    public Matricula consultar(int idMatricula);

    public Matricula cadastrar(Matricula matricula);

    public boolean alterar(Matricula matricula, int idMatricula);

    public boolean excluir(int idMatricula);

    public Matricula consultarAtivaDoAluno(int idAluno, int anoLetivo);

    public ArrayList<Matricula> consultarPorAluno(int idAluno);

    public ArrayList<Matricula> consultarAtivasDaTurma(int idTurma);
}

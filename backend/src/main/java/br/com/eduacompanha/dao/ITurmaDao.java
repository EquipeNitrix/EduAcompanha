package br.com.eduacompanha.dao;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.Turno;

public interface ITurmaDao {
    public ArrayList<Turma> consultar();

    public Turma consultar(int idTurma);

    public Turma cadastrar(Turma turma);

    public boolean alterar(Turma turma, int idTurma);

    public boolean excluir(int idTurma);

    public List<Turma> consultarPorEscola(String cie, int anoLetivo);

    public List<Turma> consultarPorEscolaTurno(String cie, Turno turno, int anoLetivo);

}

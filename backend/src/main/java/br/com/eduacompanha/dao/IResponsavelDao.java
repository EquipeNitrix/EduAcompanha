package br.com.eduacompanha.dao;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Responsavel;

public interface IResponsavelDao {
    public ArrayList<Responsavel> consultar();

    public Responsavel consultar(int idResponsavel);

    public Responsavel cadastrar(Responsavel responsavel);

    public boolean alterar(Responsavel responsavel, int idResponsavel);

    public boolean excluir(int idResponsavel);

    public List<Responsavel> consultarPorAluno(int idAluno);
}

package br.com.eduacompanha.dao;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.AcompanhamentoCaso;

public interface IAcompanhamentoCasoDao {
    public ArrayList<AcompanhamentoCaso> consultar();

    public AcompanhamentoCaso consultar(int idAcompanhamento);

    public AcompanhamentoCaso cadastrar(AcompanhamentoCaso acompanhamentoCaso);

    public boolean alterar(AcompanhamentoCaso acompanhamentoCaso, int idAcompanhamento);

    public boolean excluir(int idAcompanhamento);

    public List<AcompanhamentoCaso> consultarPorCaso(int idCaso);
}

package br.com.eduacompanha.dao;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.CasoBuscaAtiva;
import br.com.eduacompanha.model.enums.StatusCaso;

public interface ICasoBuscaAtivaDao {
    public ArrayList<CasoBuscaAtiva> consultar();

    public CasoBuscaAtiva consultar(int idCaso);

    public CasoBuscaAtiva cadastrar(CasoBuscaAtiva casoBuscaAtiva);

    public boolean alterar(CasoBuscaAtiva casoBuscaAtiva, int idCaso);

    public boolean excluir(int idCaso);

    public CasoBuscaAtiva consultarAbertoDoAluno(int idAluno);

    public List<CasoBuscaAtiva> consultarPorEscola(String cie, StatusCaso status);
}

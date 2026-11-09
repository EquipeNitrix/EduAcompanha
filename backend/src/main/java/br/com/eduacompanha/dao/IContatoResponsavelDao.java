package br.com.eduacompanha.dao;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.ContatoResponsavel;

public interface IContatoResponsavelDao {
    public ArrayList<ContatoResponsavel> consultar();

    public ContatoResponsavel consultar(int idContato);

    public ContatoResponsavel cadastrar(ContatoResponsavel contatoResponsavel);

    public boolean alterar(ContatoResponsavel contatoResponsavel, int idContato);

    public boolean excluir(int idContato);

    public List<ContatoResponsavel> consultarPorAluno(int idAluno);

    public boolean alterarPosicao(int idContato, int posicao);

    public boolean trocarPosicoes(int idContatoA, int idContatoB);

}

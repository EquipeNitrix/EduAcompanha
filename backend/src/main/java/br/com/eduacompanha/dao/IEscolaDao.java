package br.com.eduacompanha.dao;

import java.util.ArrayList;
import br.com.eduacompanha.model.Escola;

public interface IEscolaDao {
    public ArrayList<Escola> consultar();

    public Escola consultar(String cie);

    public Escola cadastrar(Escola escola);

    public boolean alterar(Escola escola, String cie);

    public boolean excluir(String cie);
}

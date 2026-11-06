package br.com.eduacompanha.dao;

import java.util.ArrayList;
import br.com.eduacompanha.model.AnoLetivo;

public interface IAnoLetivoDao {
    public ArrayList<AnoLetivo> consultar();

    public AnoLetivo consultar(int ano);

    public AnoLetivo cadastrar(AnoLetivo anoLetivo);

    public boolean alterar(AnoLetivo anoLetivo, int ano);

    public boolean excluir(int ano);
}

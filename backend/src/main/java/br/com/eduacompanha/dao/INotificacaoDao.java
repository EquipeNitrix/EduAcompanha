package br.com.eduacompanha.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Notificacao;

public interface INotificacaoDao {
    public ArrayList<Notificacao> consultar();

    public Notificacao consultar(int idNotificacao);

    public Notificacao cadastrar(Notificacao notificacao);

    public boolean alterar(Notificacao notificacao, int idNotificacao);

    public boolean excluir(int idNotificacao);
    
    public List<Notificacao> consultarAguardando();
    
    public Notificacao consultarPorMatriculaData(int idMatricula, LocalDate data);
    
}

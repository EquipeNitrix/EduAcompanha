package br.com.eduacompanha.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Notificacao;

public class NotificacaoDao implements INotificacaoDao {

    @Override
    public ArrayList<Notificacao> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Notificacao consultar(int idNotificacao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public Notificacao cadastrar(Notificacao notificacao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(Notificacao notificacao, int idNotificacao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idNotificacao) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<Notificacao> consultarAguardando() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarAguardando'");
    }

    @Override
    public Notificacao consultarPorMatriculaData(int idMatricula, LocalDate data) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorMatriculaData'");
    }


}

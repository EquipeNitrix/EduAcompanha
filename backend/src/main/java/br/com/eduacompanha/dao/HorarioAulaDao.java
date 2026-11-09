package br.com.eduacompanha.dao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.factory.ConnectionFactory;
import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.enums.Turno;

public class HorarioAulaDao implements IHorarioAulaDao{
    private Connection conexao;
    public HorarioAulaDao() {
    conexao = new ConnectionFactory().getConnection();
}

    @Override
    public ArrayList<HorarioAula> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public HorarioAula consultar(int idHorario) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public HorarioAula cadastrar(HorarioAula horarioAula) {
       
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(HorarioAula horarioAula, int idHorario) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idHorario) {
       
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<HorarioAula> consultarPorEscolaTurno(String cie, Turno turno) {
      
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorEscolaTurno'");
    }

    @Override
    public void cadastrarVersao(String cie, Turno turno, LocalDate vigencia, List<HorarioAula> aulas) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrarVersao'");
    }

    @Override
    public boolean excluirVersao(String cie, Turno turno, LocalDate vigencia) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluirVersao'");
    }
    
}

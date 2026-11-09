package br.com.eduacompanha.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.HorarioAula;
import br.com.eduacompanha.model.enums.Turno;

public interface IHorarioAulaDao {
    public ArrayList<HorarioAula> consultar();

    public HorarioAula consultar(int idHorario);

    public HorarioAula cadastrar(HorarioAula horarioAula);

    public boolean alterar(HorarioAula horarioAula, int idHorario);

    public boolean excluir(int idHorario);

    public List<HorarioAula> consultarPorEscolaTurno(String cie, Turno turno);

    void cadastrarVersao(String cie, Turno turno, LocalDate vigencia, List<HorarioAula> aulas);

    boolean excluirVersao(String cie, Turno turno, LocalDate vigencia);

}

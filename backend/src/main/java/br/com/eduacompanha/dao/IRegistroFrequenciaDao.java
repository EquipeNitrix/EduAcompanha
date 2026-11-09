package br.com.eduacompanha.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.RegistroFrequencia;

public interface IRegistroFrequenciaDao {
    public ArrayList<RegistroFrequencia> consultar();

    public RegistroFrequencia consultar(int idRegistroFreq);

    public RegistroFrequencia cadastrar(RegistroFrequencia registroFrequencia);

    public boolean alterar(RegistroFrequencia registroFrequencia, int idRegistroFreq);

    public boolean excluir(int idRegistroFreq);

    public List<RegistroFrequencia> consultarPorMatricula(int idMatricula);

    public List<RegistroFrequencia> consultarDoDia(int idMatricula, LocalDate data);

    public boolean cancelar(int idRegistro, String motivo, int idUsuario, LocalDateTime canceladoEm);
}

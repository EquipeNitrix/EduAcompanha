package br.com.eduacompanha.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.RegistroFrequencia;

public class RegistroFrequenciaDao implements IRegistroFrequenciaDao {

    @Override
    public ArrayList<RegistroFrequencia> consultar() {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public RegistroFrequencia consultar(int idRegistroFreq) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultar'");
    }

    @Override
    public RegistroFrequencia cadastrar(RegistroFrequencia registroFrequencia) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cadastrar'");
    }

    @Override
    public boolean alterar(RegistroFrequencia registroFrequencia, int idRegistroFreq) {
        
        throw new UnsupportedOperationException("Unimplemented method 'alterar'");
    }

    @Override
    public boolean excluir(int idRegistroFreq) {
        
        throw new UnsupportedOperationException("Unimplemented method 'excluir'");
    }

    @Override
    public List<RegistroFrequencia> consultarPorMatricula(int idMatricula) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarPorMatricula'");
    }

    @Override
    public List<RegistroFrequencia> consultarDoDia(int idMatricula, LocalDate data) {
        
        throw new UnsupportedOperationException("Unimplemented method 'consultarDoDia'");
    }

    @Override
    public boolean cancelar(int idRegistro, String motivo, int idUsuario, LocalDateTime canceladoEm) {
        
        throw new UnsupportedOperationException("Unimplemented method 'cancelar'");
    }
    
}

package br.com.eduacompanha.dao;

import java.util.ArrayList;
import java.util.List;

import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.ContatoResponsavel;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Responsavel;

public interface IAlunoDao {
    public ArrayList<Aluno> consultar();

    public Aluno consultar(int idAluno);

    public Aluno cadastrar(Aluno aluno);

    public boolean alterar(Aluno aluno, int idAluno);

    public boolean excluir(int idAluno);

    public Aluno consultarPorRa(String ra);

    public List<Aluno> consultarPorNome(String trecho);

    public Aluno cadastrarCompleto(Aluno aluno, Matricula matricula, Responsavel responsavel,
            List<ContatoResponsavel> telefones);

}

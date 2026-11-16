package br.com.eduacompanha.service;

import java.time.Clock;

import br.com.eduacompanha.dao.IEscolaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.util.SessaoUtil;

public class TurmaService {
    private IEscolaDao escolaDao;
    private ITurmaDao turmaDao;
    private Clock relogio;

    public TurmaService(IEscolaDao escolaDao, ITurmaDao turmaDao, Clock relogio) {
        this.escolaDao = escolaDao;
        this.turmaDao = turmaDao;
        this.relogio = relogio;
    }

    public Turma salvar(Turma t) {
        SessaoUtil.exigirAdmin();
        boolean nova = t.getIdTurma() == 0;

        /*
         * RN-17: ano atual na turma nova / na edição, escola e ano não mudam / ano
         * encerrado não muda
         * RN-18: turno do modelo da escola
         * RN-19: nome único na escola e no ano
         * gravar
         */

        return null;
    }
}

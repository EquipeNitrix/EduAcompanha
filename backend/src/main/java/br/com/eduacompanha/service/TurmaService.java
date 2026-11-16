package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.eduacompanha.dao.IEscolaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.enums.ModeloTurno;
import br.com.eduacompanha.model.enums.Turno;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.util.TextoUtil;

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
        String nome = TextoUtil.limparEspacos(t.getNomeTurma());
        Turno turno = t.getTurno();
        String cie = t.getCie();

        Map<String, String> erros = new LinkedHashMap<>();

        if (nome == null) {
            erros.put("nome", "Informe o nome da turma.");
        }
        if (turno == null) {
            erros.put("turno", "Escolha o turno.");
        }
        if (cie == null) {
            erros.put("escolaId", "Escolha a escola.");
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        int anoAtual = LocalDate.now(relogio).getYear();
        Turma anterior = null;
        if (!nova) {
            anterior = turmaDao.consultar(t.getIdTurma());
        }
        if (!nova && anterior == null) {
            throw new NaoEncontradoException("Turma não encontrada.");
        }

        if (anterior != null && anterior.getAnoLetivo() != anoAtual) {
            throw new RegraNegocioException("Turmas de anos letivos encerrados não podem ser alteradas.");
        }

        int ano = (nova ? anoAtual : anterior.getAnoLetivo());
        String cieFinal = (nova ? cie : anterior.getCie());

        Escola escola = escolaDao.consultar(cieFinal);
        if (escola == null) {
            throw new NaoEncontradoException("Escola não encontrada");
        }

        if (!escola.___().___().contains(___)) {
            String mensagem = "Esta escola usa o modelo \"" + ___ + "\". Escolha "
                    + (___ == ModeloTurno.INTEGRAL ? "o turno Integral." : "manhã, tarde ou noite.");
            throw new ValidacaoException("turno", mensagem);
        }

        /*
         * RN-18: turno do modelo da escola
         * RN-19: nome único na escola e no ano
         * gravar
         */

        return null;
    }
}

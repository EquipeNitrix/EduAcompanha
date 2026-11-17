package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.eduacompanha.dao.IAlunoDao;
import br.com.eduacompanha.dao.IAnoLetivoDao;
import br.com.eduacompanha.dao.IContatoResponsavelDao;
import br.com.eduacompanha.dao.IMatriculaDao;
import br.com.eduacompanha.dao.IResponsavelDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.model.Aluno;
import br.com.eduacompanha.model.AnoLetivo;
import br.com.eduacompanha.model.ContatoResponsavel;
import br.com.eduacompanha.model.Matricula;
import br.com.eduacompanha.model.Responsavel;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.util.TextoUtil;

public class AlunoService {
    private IAlunoDao alunoDao;
    private ITurmaDao turmaDao;
    private IAnoLetivoDao anoLetivoDao;
    private IResponsavelDao responsavelDao;
    private IContatoResponsavelDao contatoResponsavelDao;
    private IMatriculaDao matriculaDao;
    private Clock relogio;

    private static final List<String> PARENTESCOS = List.of("Mãe", "Pai", "Avó", "Avô", "Tia", "Tio",
            "Irmã", "Irmão", "Responsável legal", "Outro");

    public AlunoService(IAlunoDao alunoDao, ITurmaDao turmaDao, IAnoLetivoDao anoLetivoDao,
            IResponsavelDao responsavelDao, IContatoResponsavelDao contatoResponsavelDao, IMatriculaDao matriculaDao,
            Clock relogio) {
        this.alunoDao = alunoDao;
        this.turmaDao = turmaDao;
        this.anoLetivoDao = anoLetivoDao;
        this.responsavelDao = responsavelDao;
        this.contatoResponsavelDao = contatoResponsavelDao;
        this.matriculaDao = matriculaDao;
        this.relogio = relogio;
    }

    public Aluno cadastrar(Aluno aluno, Matricula matricula, Responsavel responsavel,
            String telefone1, String telefone2) {
        SessaoUtil.exigirAdmin();

        String ra = aluno.getRa() == null ? "" : aluno.getRa().trim().replace("-", "").toUpperCase();
        String nome = TextoUtil.limparEspacos(aluno.getNomeAluno());

        Map<String, String> erros = new LinkedHashMap<>();

        if (ra.equals("")) {
            erros.put("ra", "Informe o RA.");
        } else if (!ra.matches("\\d{12}[\\dX]")) {
            erros.put("ra", "Use só números, com o dígito no final. Exemplo: 000123456789-0");
        }

        if (nome == null) {
            erros.put("nome", "Informe o nome completo.");
        } else if (!nome.contains(" ")) {
            erros.put("nome", "Informe nome e sobrenome.");
        }

        if (matricula.getIdTurma() == 0) {
            erros.put("turma", "Escolha a turma.");
        }

        int anoAtual = LocalDate.now(relogio).getYear();
        AnoLetivo anoLetivo = anoLetivoDao.consultar(anoAtual);
        LocalDate inicio = matricula.getDataInicio();
        if (inicio == null) {
            erros.put("inicio", "Informe a data de início.");
        } else if (inicio.isBefore(anoLetivo.getDataInicio()) || inicio.isAfter(anoLetivo.getDataFim())) {
            DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            erros.put("inicio", "Use uma data entre " + br.format(anoLetivo.getDataInicio()) + " e "
                    + br.format(anoLetivo.getDataFim()) + ".");
        }

        String nomeResponsavel = TextoUtil.limparEspacos(responsavel.getNomeResponsavel());
        if (nomeResponsavel == null) {
            erros.put("responsavel", "Informe o nome do responsável.");
        }
        if (responsavel.getParentesco() == null || !PARENTESCOS.contains(responsavel.getParentesco())) {
            erros.put("parentesco", "Escolha o parentesco.");
        }

        String tel1 = TextoUtil.limparEspacos(telefone1);
        String tel2 = TextoUtil.limparEspacos(telefone2);
        String formato = "\\(\\d{2}\\) 9?\\d{4}-\\d{4}";

        if (tel1 == null) {
            erros.put("telefone1", "Informe o telefone.");
        } else if (!tel1.matches(formato)) {
            erros.put("telefone1", "Use o formato (15) 99999-9999.");
        }

        if (tel2 != null) {
            if (!tel2.matches(formato)) {
                erros.put("telefone2", "Use o formato (15) 99999-9999.");
            } else if (tel2.equals(tel1)) {
                erros.put("telefone2", "Os telefones precisam ser diferentes.");
            }
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        if (alunoDao.consultarPorRa(ra) != null) {
            throw new ValidacaoException("ra", "Já existe um aluno com este RA.");
        }

        Turma turma = turmaDao.consultar(matricula.getIdTurma());
        if (turma == null || turma.getAnoLetivo() != anoAtual) {
            throw new ValidacaoException("turma", "Escolha uma turma do ano letivo atual.");
        }

        aluno.setRa(ra);
        aluno.setNomeAluno(nome);
        responsavel.setNomeResponsavel(nomeResponsavel);

        List<ContatoResponsavel> telefones = new ArrayList<>();
        telefones.add(new ContatoResponsavel(tel1, 1, 0));
        if (tel2 != null) {
            telefones.add(new ContatoResponsavel(tel2, 2, 0));
        }

        return alunoDao.cadastrarCompleto(aluno, matricula, responsavel, telefones);

    }

    /* 
    public Aluno editar(int idAluno, String raDigitado, String nomeDigitado) {
        Aluno aluno = alunoDao.consultar(idAluno);
        if (aluno == null) {
            throw new NaoEncontradoException("Aluno não encontrado.");
        }

        
        SessaoUtil.exigirAcessoEscola(escolaDoAluno(idAluno));

        // 3. limpar e validar: copie do cadastrar as linhas do ra, do nome,
        //    o Map de erros, os dois if de validação e o throw se tiver erro
        //    (troque aluno.getRa() por raDigitado e aluno.getNomeAluno() por nomeDigitado)
        ________

        // 4. RA repetido, ignorando o próprio aluno
        Aluno outro = alunoDao.consultarPorRa(ra);
        if (outro != null && ________) {
            throw new ValidacaoException("ra", "Já existe um aluno com este RA.");
        }

        // 5. gravar
        aluno.setRa(ra);
        aluno.setNomeAluno(nome);
        alunoDao.________(aluno, idAluno);
        return aluno;
    }

    private String escolaDoAluno(int idAluno) {
        List<Matricula> matriculas = matriculaDao.consultarPorAluno(idAluno);
        if (matriculas.isEmpty()) {
            return null;
        }
        Matricula maisRecente = matriculas.get(0);
        Turma turma = turmaDao.consultar(maisRecente.getIdTurma());
        return turma.getCie();
    }*/

}

package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.eduacompanha.dao.IEscolaDao;
import br.com.eduacompanha.dao.ITurmaDao;
import br.com.eduacompanha.dao.IUsuarioDao;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.Turma;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.ModeloTurno;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.model.enums.Turno;

public class EscolaService {
    private IEscolaDao escolaDao;
    private ITurmaDao turmaDao;
    private IUsuarioDao usuarioDao;
    private Clock relogio;

    public EscolaService(IEscolaDao escolaDao, ITurmaDao turmaDao, IUsuarioDao usuarioDao, Clock relogio) {
        this.escolaDao = escolaDao;
        this.turmaDao = turmaDao;
        this.usuarioDao = usuarioDao;
        this.relogio = relogio;
    }

    private void exigirAdmin(Usuario quem) {
        if (quem == null || quem.getPerfil() != Perfil.ADMIN) {
            throw new RuntimeException("Seu perfil não tem permissão para esta ação.");
        }
    }

    private String limparEspacos(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim().replaceAll("\\s+", " ");
        if (limpo.equals("")) {
            return null;
        }
        return limpo;
    }

    private List<Turno> turnosDoModelo(ModeloTurno modelo) {
        if (modelo == ModeloTurno.INTEGRAL) {
            return List.of(Turno.INTEGRAL);
        } else {
            return List.of(Turno.MANHA, Turno.TARDE, Turno.NOITE);
        }
    }

    private String nomeDoModelo(ModeloTurno modelo) {
        if (modelo == ModeloTurno.INTEGRAL) {
            return "Integral";
        } else {
            return "Manhã, tarde e noite";
        }
    }

    public Escola salvar(Usuario quem, Escola dados, boolean nova) {
        exigirAdmin(quem);
        String cie = dados.getCie();
        String nome = dados.getNomeEscola();
        ModeloTurno modelo = dados.getModeloTurno();

        if (cie == null) {
            cie = "";
        } else {
            cie = cie.trim();
        }

        nome = limparEspacos(nome);

        Map<String, String> erros = new LinkedHashMap<>();

        if (cie.equals("")) {
            erros.put("cie", "Informe o código CIE.");
        } else if (!cie.matches("\\d{6}")) {
            erros.put("cie", "O CIE tem 6 números.");
        }

        if (nome == null) {
            erros.put("nome", "Informe o nome da escola.");
        }

        if (modelo == null) {
            erros.put("modeloTurno", "Escolha o modelo de turno.");
        }

        if (!erros.isEmpty()) {
            throw new RuntimeException(erros.toString());
        }

        Escola anterior = escolaDao.consultar(cie);

        if (nova && anterior != null) {
            throw new RuntimeException("Já existe uma escola com este CIE.");
        }
        if (!nova && anterior == null) {
            throw new RuntimeException("Escola não encontrada.");
        }

        if (!nova && anterior.getModeloTurno() != modelo) {
            int anoAtual = LocalDate.now(relogio).getYear();

            for (Turma t : turmaDao.consultarPorEscola(cie, anoAtual)) {
                if (!turnosDoModelo(modelo).contains(t.getTurno())) {
                    throw new RuntimeException("A escola tem turmas de " + anoAtual
                            + " em turnos do modelo atual. Para mudar o modelo, "
                            + "as turmas precisam estar no turno do novo modelo.");
                }
            }
        }

        Escola escola = new Escola();
        escola.setCie(cie);
        escola.setNomeEscola(nome);
        escola.setMunicipio(limparEspacos(dados.getMunicipio()));
        escola.setModeloTurno(modelo);

        for (Turno turno : Turno.values()) {
            if (turnosDoModelo(modelo).contains(turno)) {
                escola.setHorarioEnvio(turno, dados.getHorarioEnvio(turno));
            } else if (anterior != null) {
                escola.setHorarioEnvio(turno, anterior.getHorarioEnvio(turno));
            }
        }

        if (nova) {
            return escolaDao.cadastrar(escola);
        }
        escolaDao.alterar(escola, cie);
        return escola;
    }

    public record Resumo(Escola escola, int usuarios, int turmas, int turnosPendentes, String modelo) {
    }

    public List<Resumo> listarResumo(Usuario quem) {
        exigirAdmin(quem);
        List<Resumo> resultado = new ArrayList<>();

        int anoAtual = LocalDate.now(relogio).getYear();

        ArrayList<Usuario> todosUsuarios = usuarioDao.consultar();

        for (Escola escola : escolaDao.consultar()) {
            int usuarios = 0;
            for (Usuario u : todosUsuarios) {
                if (escola.getCie().equals(u.getCie())) {
                    usuarios++;
                }
            }
            List<Turma> turmas = turmaDao.consultarPorEscola(escola.getCie(), anoAtual);
            Set<Turno> turnosUsados = new LinkedHashSet<>();
            for (Turma t : turmas) {
                turnosUsados.add(t.getTurno());
            }
            int pendentes = 0;
            for (Turno turno : turnosUsados) {
                // TODO: contar também o turno sem grade
                if (escola.getHorarioEnvio(turno) == null) {
                    pendentes++;
                }
            }
            resultado
                    .add(new Resumo(escola, usuarios, turmas.size(), pendentes, nomeDoModelo(escola.getModeloTurno())));
        }

        return resultado;
    }
}

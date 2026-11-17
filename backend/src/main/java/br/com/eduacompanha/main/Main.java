package br.com.eduacompanha.main;

import java.time.Clock;
import java.time.ZoneId;

import br.com.eduacompanha.dao.AcompanhamentoCasoDao;
import br.com.eduacompanha.dao.AlunoDao;
import br.com.eduacompanha.dao.AnoLetivoDao;
import br.com.eduacompanha.dao.CasoBuscaAtivaDao;
import br.com.eduacompanha.dao.ContatoResponsavelDao;
import br.com.eduacompanha.dao.EnvioNotificacaoDao;
import br.com.eduacompanha.dao.EscolaDao;
import br.com.eduacompanha.dao.HorarioAulaDao;
import br.com.eduacompanha.dao.MatriculaDao;
import br.com.eduacompanha.dao.NotificacaoDao;
import br.com.eduacompanha.dao.RegistroFrequenciaDao;
import br.com.eduacompanha.dao.ResponsavelDao;
import br.com.eduacompanha.dao.TurmaDao;
import br.com.eduacompanha.dao.UsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.service.AcessoService;
import br.com.eduacompanha.service.AlertaService;
import br.com.eduacompanha.service.AlunoService;
import br.com.eduacompanha.service.BuscaAtivaService;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.FrequenciaService;
import br.com.eduacompanha.service.MatriculaService;
import br.com.eduacompanha.service.NotificacaoService;
import br.com.eduacompanha.service.TurmaService;
import br.com.eduacompanha.service.TurnoService;
import br.com.eduacompanha.service.UsuarioService;
import br.com.eduacompanha.view.MenuAlerta;
import br.com.eduacompanha.view.MenuAluno;
import br.com.eduacompanha.view.MenuBuscaAtiva;
import br.com.eduacompanha.view.MenuChamada;
import br.com.eduacompanha.view.MenuEscola;
import br.com.eduacompanha.view.MenuNotificacao;
import br.com.eduacompanha.view.MenuPrincipal;
import br.com.eduacompanha.view.MenuTurma;
import br.com.eduacompanha.view.MenuTurno;
import br.com.eduacompanha.view.TelaLogin;
import br.com.eduacompanha.view.MenuUsuario;

public class Main {
        public static void main(String[] args) {
                Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

                // services aq.
                TurmaService turmaService = new TurmaService(new EscolaDao(), new TurmaDao(), relogio);
                EscolaService escolaService = new EscolaService(new EscolaDao(), new TurmaDao(), new UsuarioDao(),
                                new HorarioAulaDao(), relogio);
                AcessoService acessoService = new AcessoService(new UsuarioDao(), relogio);
                TurnoService turnoService = new TurnoService(new HorarioAulaDao(), new EscolaDao(), new AnoLetivoDao(),
                                relogio);
                UsuarioService usuarioService = new UsuarioService(new UsuarioDao(), new EscolaDao());
                BuscaAtivaService buscaAtivaService = new BuscaAtivaService(new CasoBuscaAtivaDao(),
                                new AcompanhamentoCasoDao(), new AlunoDao(), new MatriculaDao(), new TurmaDao(),
                                new ResponsavelDao(), relogio);
                FrequenciaService frequenciaService = new FrequenciaService(new RegistroFrequenciaDao(),
                                new MatriculaDao(), new TurmaDao(), new AlunoDao(), new AnoLetivoDao(), turnoService,
                                relogio);
                AlertaService alertaService = new AlertaService(frequenciaService, new MatriculaDao(),
                                new TurmaDao(), new AnoLetivoDao(), new RegistroFrequenciaDao(), relogio);
                NotificacaoService notificacaoService = new NotificacaoService(new NotificacaoDao(),
                                new EnvioNotificacaoDao(), new ContatoResponsavelDao(), new RegistroFrequenciaDao(),
                                new MatriculaDao(), new TurmaDao(), new AlunoDao(), new EscolaDao(), relogio);
                AlunoService alunoService = new AlunoService(new AlunoDao(), new TurmaDao(), new AnoLetivoDao(),
                                new ResponsavelDao(), new ContatoResponsavelDao(), new MatriculaDao(), relogio);
                MatriculaService matriculaService = new MatriculaService(new MatriculaDao(), new AlunoDao(),
                                new TurmaDao(), new AnoLetivoDao(), relogio);

                // telas aq.
                TelaLogin telaLogin = new TelaLogin(acessoService);
                MenuPrincipal menuPrincipal = new MenuPrincipal(acessoService,
                                new MenuEscola(escolaService),
                                new MenuTurma(escolaService, turmaService),
                                new MenuTurno(escolaService, turnoService),
                                new MenuUsuario(usuarioService, escolaService),
                                new MenuBuscaAtiva(buscaAtivaService),
                                new MenuChamada(frequenciaService),
                                new MenuAlerta(escolaService, alertaService),
                                new MenuNotificacao(notificacaoService),
                                new MenuAluno(escolaService, turmaService, matriculaService, alunoService));

                // login aq.
                while (true) {
                        Usuario usuario = telaLogin.abrir();
                        if (usuario == null) {
                                return;
                        }
                        menuPrincipal.abrir();
                }

        }
}
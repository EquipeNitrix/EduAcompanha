package br.com.eduacompanha.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import br.com.eduacompanha.dao.IUsuarioDao;
import br.com.eduacompanha.dao.UsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.util.SenhaUtil;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.util.TextoUtil;

public class AcessoService {

    private static final int MAX_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;

    private IUsuarioDao usuarioDao;
    private Clock relogio;

    public AcessoService(IUsuarioDao usuarioDao, Clock relogio) {
        this.usuarioDao = usuarioDao;
        this.relogio = relogio;
    }

    public Usuario login(String login, String senha) {
        // login e senha obrigatorio
        if (login == null || login.trim().equals("") || senha == null || senha.equals("")) {
            throw new ValidacaoException("login", "Informe o login e a senha. ");
        }

        // compara sem acento, em minuscula e espaço nas ponta
        Usuario usuario = usuarioDao.consultarPorLogin(TextoUtil.normalizar(login));
        LocalDateTime agora = LocalDateTime.now(relogio);

        // se foi bloqueado, recusa antes de ver a senha
        if (usuario != null && usuario.getBloqueadoAte() != null && agora.isBefore(usuario.getBloqueadoAte())) {
            long segundos = Duration.between(agora, usuario.getBloqueadoAte()).getSeconds();
            long minutos = (segundos + 59) / 60; // para arredondar, 61s = 2min
            throw new RegraNegocioException("Acesso bloqueado após " + MAX_TENTATIVAS
                    + "tentativas incorretas. Tente de novo em " + minutos);
        }

        // login não existe, inativo, senha errado dão a msm mensagem de erro
        if (usuario == null || !usuario.isAtivo() || !usuario.getSenhaHash().equals(SenhaUtil.gerarHash(senha))) {
            if (usuario != null) {
                registrarFalha(usuario, agora);
            }
            throw new RegraNegocioException("Login ou senha incorretos. ");
        }

        // se acertar, zero falhas e bloqueio
        usuarioDao.atualizarTentativas(usuario.getIdUsuario(), 0, null);
        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        usuario.setSenhaHash(null);

        SessaoUtil.entrar(usuario);
        return usuario;
    }

    public void sair() {
        SessaoUtil.sair();
    }

    // soma uma falha, na quinta, zera o contador e bloqueia 15min
    private void registrarFalha(Usuario usuario, LocalDateTime agora) {
        int falhas = usuario.getTentativasFalhas() + 1;
        if (falhas >= MAX_TENTATIVAS) {
            usuarioDao.atualizarTentativas(usuario.getIdUsuario(), 0, agora.plusMinutes(MINUTOS_BLOQUEIO));
            throw new RegraNegocioException("Acesso bloqueado por " + MINUTOS_BLOQUEIO + "minutos após "
                    + MAX_TENTATIVAS + "tentativas incorretas.");
        }
        usuarioDao.atualizarTentativas(usuario.getIdUsuario(), falhas, null);
    }

}

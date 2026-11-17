package br.com.eduacompanha.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.eduacompanha.dao.IEscolaDao;
import br.com.eduacompanha.dao.IUsuarioDao;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.util.SenhaUtil;
import br.com.eduacompanha.util.SessaoUtil;
import br.com.eduacompanha.util.TextoUtil;

public class UsuarioService {
    private IUsuarioDao usuarioDao;
    private IEscolaDao escolaDao;

    public UsuarioService(IUsuarioDao usuarioDao, IEscolaDao escolaDao) {
        this.usuarioDao = usuarioDao;
        this.escolaDao = escolaDao;
    }

    // lista para a tela
    public List<Usuario> listar() {
        SessaoUtil.exigirAdmin();
        List<Usuario> admins = new ArrayList<>();
        List<Usuario> secretarias = new ArrayList<>();
        for (Usuario u : usuarioDao.consultar()) {
            u.setSenhaHash(null); // hash fica só no service
            if (u.getPerfil() == Perfil.ADMIN) {
                admins.add(u);
            } else {
                secretarias.add(u);
            }
        }

        admins.sort((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
        secretarias.sort((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
        admins.addAll(secretarias);
        return admins;
    }

    // criar sempore usuario ativo da escola
    public Usuario criar(String nome, String login, String cie, String senha, String senha2) {
        SessaoUtil.exigirAdmin();

        String nomeLimpo = TextoUtil.limparEspacos(nome);
        String loginLimpo = TextoUtil.normalizar(login);

        // junta todos os erros e mostra de uma vez
        Map<String, String> erros = new LinkedHashMap<>();
        if (nomeLimpo == null) {
            erros.put("nome", "Informe o nome.");
        }
        if (loginLimpo == null || loginLimpo.equals("")) {
            erros.put("login", "Informe o login.");
        } else if (!loginLimpo.matches("[a-z0-9.]{4,40}")) {
            erros.put("login", "Use de 4 a 40 caracteres: letras minúsculas, números e ponto.");
        }
        if (cie == null || cie.equals("")) {
            erros.put("escola", "Escolha a escola. Todo usuário da secretaria pertence a uma escola.");
        } else if (escolaDao.consultar(cie) == null) {
            erros.put("escola", "Escola não encontrada.");
        }
        validarSenha(senha, senha2, erros);
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        // login único
        if (usuarioDao.consultarPorLogin(loginLimpo) != null) {
            throw new ValidacaoException("login", "Este login já está em uso.");
        }

        Usuario u = new Usuario();
        u.setNome(nomeLimpo);
        u.setLogin(loginLimpo);
        u.setSenhaHash(SenhaUtil.gerarHash(senha)); // gravar o hash, nunca a senha
        u.setPerfil(Perfil.SECRETARIA);
        u.setAtivo(true);
        u.setTentativasFalhas(0);
        u.setBloqueadoAte(null);
        u.setCie(cie);
        usuarioDao.cadastrar(u);

        u.setSenhaHash(null);
        return u;
    }

    // muda nome, escola e se está ativo.
    public Usuario editar(int idUsuario, String nome, String cie, boolean ativo) {
        SessaoUtil.exigirAdmin();
        Usuario u = buscarSecretaria(idUsuario);

        String nomeLimpo = TextoUtil.limparEspacos(nome);
        Map<String, String> erros = new LinkedHashMap<>();
        if (nomeLimpo == null) {
            erros.put("nome", "Informe o nome.");
        }
        if (cie == null || cie.equals("")) {
            erros.put("escola", "Escolha a escola.");
        } else if (escolaDao.consultar(cie) == null) {
            erros.put("escola", "Escola não encontrada.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        u.setNome(nomeLimpo);
        u.setCie(cie);
        u.setAtivo(ativo);
        usuarioDao.alterar(u, idUsuario); // o hash veio do banco no buscar, então a senha continua a mesma

        u.setSenhaHash(null);
        return u;
    }

    public void definirSenha(int idUsuario, String senha, String senha2) {
        SessaoUtil.exigirAdmin(); // RN-06
        Usuario u = usuarioDao.consultar(idUsuario);
        if (u == null) {
            throw new NaoEncontradoException("Usuário não encontrado.");
        }

        Map<String, String> erros = new LinkedHashMap<>();
        validarSenha(senha, senha2, erros);
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        u.setSenhaHash(SenhaUtil.gerarHash(senha));
        u.setTentativasFalhas(0);
        u.setBloqueadoAte(null);
        usuarioDao.alterar(u, idUsuario);
    }

    public void desbloquear(int idUsuario) {
        SessaoUtil.exigirAdmin();
        if (usuarioDao.consultar(idUsuario) == null) {
            throw new NaoEncontradoException("Usuário não encontrado.");
        }
        usuarioDao.atualizarTentativas(idUsuario, 0, null);
    }

    // 8+ caracteres, pelo menos uma letra e um número;
    private void validarSenha(String senha, String senha2, Map<String, String> erros) {
        if (senha == null || senha.length() < 8 || !senha.matches(".*[a-zA-Z].*") || !senha.matches(".*\\d.*")) {
            erros.put("senha", "Use pelo menos 8 caracteres, com letras e números.");
        } else if (!senha.equals(senha2)) {
            erros.put("senha2", "As senhas não conferem.");
        }
    }

    // a tela de usuários só mexe em contas da secretaria
    private Usuario buscarSecretaria(int idUsuario) {
        Usuario u = usuarioDao.consultar(idUsuario);
        if (u == null) {
            throw new NaoEncontradoException("Usuário não encontrado.");
        }
        if (u.getPerfil() != Perfil.SECRETARIA) {
            throw new RegraNegocioException("A Conta da Equipe Nitrix não é editada por esta tela.");
        }
        return u;
    }
}

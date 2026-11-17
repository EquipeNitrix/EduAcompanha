package br.com.eduacompanha.view;

import java.util.List;

import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.Usuario;
import br.com.eduacompanha.model.enums.Perfil;
import br.com.eduacompanha.service.EscolaService;
import br.com.eduacompanha.service.UsuarioService;

public class MenuUsuario {
    private UsuarioService usuarioService;
    private EscolaService escolaService;

    public MenuUsuario(UsuarioService usuarioService, EscolaService escolaService) {
        this.usuarioService = usuarioService;
        this.escolaService = escolaService;
    }

    public void abrir() {
        while (true) {
            int escolha = Tela.escolher("Usuários", "Listar", "Criar", "Editar", "Definir senha", "Desbloquear",
                    "Voltar");
            if (escolha == 5 || escolha == -1) {
                return;
            }
            switch (escolha) {
                case 0:
                    listar();
                    break;
                case 1:
                    criar();
                    break;
                case 2:
                    editar();
                    break;
                case 3:
                    definirSenha();
                    break;
                case 4:
                    desbloquear();
                    break;
            }
        }
    }

    private void listar() {
        try {
            String texto = "";
            for (Usuario u : usuarioService.listar()) {
                texto += u.getNome() + " | " + u.getLogin()
                        + " | " + (u.getPerfil() == Perfil.ADMIN ? "Equipe Nitrix" : "Secretaria " + u.getCie())
                        + " | " + (u.isAtivo() ? "Ativo" : "Inativo")
                        + (u.getBloqueadoAte() != null ? " | Bloqueado até " + u.getBloqueadoAte().toLocalTime() : "")
                        + "\n";
            }
            Tela.mostrarLista("Usuários", texto);
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void criar() {
        try {
            String nome = Tela.pedirTexto("Nome:");
            if (nome == null) {
                return; // Cancelar em qualquer pergunta desiste do cadastro
            }
            String login = Tela.pedirTexto("Login (4 a 40 caracteres: letras minúsculas, números e ponto):");
            if (login == null) {
                return;
            }
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            String senha = Tela.pedirSenha("Senha (8 ou mais caracteres, com letras e números)");
            if (senha == null) {
                return;
            }
            String senha2 = Tela.pedirSenha("Confirme a senha");
            if (senha2 == null) {
                return;
            }

            Usuario novo = usuarioService.criar(nome, login, escola.getCie(), senha, senha2);
            Tela.mostrar("Usuário " + novo.getLogin() + " criado.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void editar() {
        try {
            Usuario u = escolherUsuario("Qual usuário vai editar?");
            if (u == null) {
                return;
            }
            String nome = Tela.pedirTexto("Nome:", u.getNome());
            if (nome == null) {
                return;
            }
            Escola escola = escolherEscola();
            if (escola == null) {
                return;
            }
            int situacao = Tela.escolher("Situação da conta", "Ativo", "Inativo");
            if (situacao == -1) {
                return;
            }

            usuarioService.editar(u.getIdUsuario(), nome, escola.getCie(), situacao == 0);
            Tela.mostrar("Usuário alterado.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void definirSenha() {
        try {
            Usuario u = escolherUsuario("De qual usuário vai trocar a senha?");
            if (u == null) {
                return;
            }
            String senha = Tela.pedirSenha("Nova senha (8 ou mais caracteres, com letras e números)");
            if (senha == null) {
                return;
            }
            String senha2 = Tela.pedirSenha("Confirme a nova senha");
            if (senha2 == null) {
                return;
            }

            usuarioService.definirSenha(u.getIdUsuario(), senha, senha2);
            Tela.mostrar("Senha alterada. O usuário também foi desbloqueado.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    private void desbloquear() {
        try {
            Usuario u = escolherUsuario("Qual usuário vai desbloquear?");
            if (u == null) {
                return;
            }
            usuarioService.desbloquear(u.getIdUsuario());
            Tela.mostrar(u.getNome() + " desbloqueado.");
        } catch (Exception e) {
            Tela.erro(e);
        }
    }

    // a caixa mostra cada usuário pelo toString
    private Usuario escolherUsuario(String titulo) {
        List<Usuario> usuarios = usuarioService.listar();
        return Tela.escolherDaLista(titulo, usuarios);
    }

    // admin vê todas as escolas
    private Escola escolherEscola() {
        return Tela.escolherDaLista("Escola do usuário:", escolaService.listarAcessiveis());
    }
}

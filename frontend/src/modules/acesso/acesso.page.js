// Telas de login, recuperação de senha e de erro (403, 404, falha).

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { navegar } from '../../app/router.js';
import { usuarioAtual } from '../../app/session.js';
import { campo } from '../../components/form.js';
import { icone } from '../../components/icons.js';
import { logo } from '../../components/logo.js';
import { botaoTema } from '../../app/tema.js';
import { entrar } from './acesso.service.js';
import { rodarRotina } from '../notificacao/notificacao.page.js';

function ladoDaMarca(titulo, texto, comPontos) {
  return `<section class="auth-brand">
    <div class="wordmark">${logo(52)}EduAcompanha</div>
    <div>
      <h1>${titulo}</h1>
      ${texto ? `<p class="lead">${texto}</p>` : ''}
      ${comPontos ? `<div class="auth-points">
        <div>${icone('check')}Registro de faltas do dia</div>
        <div>${icone('msg')}Aviso automático ao responsável</div>
        <div>${icone('compass')}Registro completo dos casos de Busca Ativa Escolar</div>
      </div>` : ''}
    </div>
  </section>`;
}

function telaLogin() {
  return `<div class="auth">
    ${ladoDaMarca('Busca Ativa escolar, <em>sem papel.</em>', '', true)}
    <section class="auth-panel">${botaoTema('no-canto')}<div class="auth-card">
      <div><h2>Entrar</h2><p class="muted">Use o login e senha fornecido</p></div>
      <form data-form="login" novalidate class="form-grid one">
        ${campo({ prefixo: 'login', rotulo: 'Login', nome: 'login', obrigatorio: true, atributos: 'autocomplete="username" autocapitalize="none" spellcheck="false"' })}
        ${campo({ prefixo: 'login', rotulo: 'Senha', nome: 'senha', tipo: 'password', obrigatorio: true, atributos: 'autocomplete="current-password"' })}
        <button class="btn btn-primary btn-block" type="submit">Entrar</button>
        <a href="#/recuperar" style="justify-self:center;font-size:14px">Esqueci minha senha</a>
      </form>
      <div class="demo-creds">
        <strong class="small">Contas de demonstração</strong>
        <div class="row">
          <button type="button" class="btn btn-ghost btn-sm" data-acao="login:preencher" data-login="conta.sec1" data-senha="Escola@2026">Secretaria</button>
          <button type="button" class="btn btn-ghost btn-sm" data-acao="login:preencher" data-login="admin.nitrix" data-senha="Nitrix@2026">Administrador</button>
          <button type="button" class="btn btn-ghost btn-sm" data-acao="login:preencher" data-login="conta.sec2" data-senha="Escola@2026">Secretaria (outra escola)</button>
        </div>
        <p class="small muted">Preenche os campos com uma conta de teste. Cinco senhas erradas seguidas bloqueiam a conta por 15 minutos.</p>
      </div>
    </div></section>
  </div>`;
}

function telaRecuperar() {
  return `<div class="auth">
    ${ladoDaMarca('Recuperar <em>acesso</em>', 'Os acessos são criados e gerenciados somente pela Equipe Nitrix.', false)}
    <section class="auth-panel">${botaoTema('no-canto')}<div class="auth-card">
      <div><h2>Esqueceu a senha?</h2>
        <p class="muted" style="margin-top:6px">A secretaria não cria nem redefine o próprio acesso. Peça ao administrador da Equipe Nitrix uma nova senha, informando seu login e a sua escola.</p></div>
      <div class="note">${icone('info')}<span>Se a conta foi bloqueada por tentativas incorretas, aguarde 15 minutos ou peça o desbloqueio ao administrador.</span></div>
      <a class="btn btn-primary" href="#/login">Voltar para o login</a>
    </div></section>
  </div>`;
}

export const telasPublicas = { login: telaLogin, recuperar: telaRecuperar };

export const telasErro = {
  negado: () => `<div class="forbidden"><div class="code">403</div><h1>Acesso negado</h1>
    <p class="muted">Seu perfil (${usuarioAtual()?.perfil === 'admin' ? 'Administrador' : 'Secretaria'}) não tem permissão para abrir esta página ou este registro.</p>
    <a class="btn btn-primary" href="#/inicio">Ir para o início</a></div>`,
  naoEncontrada: () => `<div class="forbidden"><div class="code info">404</div><h1>Página não encontrada</h1>
    <p class="muted">O endereço não existe ou o registro foi removido.</p>
    <a class="btn btn-primary" href="#/inicio">Ir para o início</a></div>`,
  falha: () => `<div class="forbidden"><h1>Não foi possível abrir esta tela</h1>
    <p class="muted">Tente novamente. Se continuar, use "Restaurar dados de exemplo" na faixa amarela.</p>
    <a class="btn btn-primary" href="#/inicio">Ir para o início</a></div>`,
};

registrarAcoes({
  'login:preencher': (d) => {
    document.getElementById('login_login').value = d.login;
    document.getElementById('login_senha').value = d.senha;
    document.querySelector('[data-form="login"] [type="submit"]').focus();
  },
});

registrarFormularios({
  login: async (dados) => {
    await entrar(dados.login, dados.senha);
    navegar('inicio');
    rodarRotina({ avisar: true });
  },
});

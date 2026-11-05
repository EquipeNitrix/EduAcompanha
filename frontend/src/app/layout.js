// Estrutura comum das telas internas: menu lateral, topo no celular e faixa de demonstração.

import { usuarioAtual } from './session.js';
import { api } from '../data/api.js';
import { icone } from '../components/icons.js';
import { logo } from '../components/logo.js';
import { botaoTema } from './tema.js';
import { esc, iniciais } from '../utils/texto.js';
import { contarAlertas } from '../modules/alerta/alerta.service.js';
import { contarCasosAbertos } from '../modules/busca-ativa/busca-ativa.service.js';
import { contarAguardando } from '../modules/notificacao/notificacao.service.js';

const MENU = {
  secretaria: [
    ['Rotina', [['inicio', 'Início', 'home'], ['frequencia', 'Lançar frequência', 'check'], ['notificacoes', 'Notificações', 'bell']]],
    ['Acompanhamento', [['alertas', 'Alerta de evasão', 'alert'], ['busca-ativa', 'Busca Ativa', 'compass']]],
    ['Consulta', [['alunos', 'Alunos', 'users'], ['turmas', 'Turmas', 'layers']]],
    ['Escola', [['turnos', 'Turnos e envio', 'clock']]],
  ],
  admin: [
    ['Visão geral', [['inicio', 'Início', 'home'], ['alertas', 'Alerta de evasão', 'alert'], ['notificacoes', 'Notificações', 'bell']]],
    ['Rede', [['escolas', 'Escolas', 'school'], ['usuarios', 'Usuários', 'usercog']]],
    ['Cadastros', [['alunos', 'Alunos', 'users'], ['turmas', 'Turmas', 'layers'], ['turnos', 'Turnos e envio', 'clock']]],
  ],
};

export function alternarMenu() { document.querySelector('.shell')?.classList.toggle('menu-aberto'); }
export function fecharMenu() { document.querySelector('.shell')?.classList.remove('menu-aberto'); }

export async function montarLayout(conteudo, ativo) {
  const usuario = usuarioAtual();
  const escola = usuario.escolaId ? await api.escolas.obter(usuario.escolaId) : null;
  const [alertas, casos, aguardando] = await Promise.all([
    contarAlertas(),
    usuario.perfil === 'secretaria' ? contarCasosAbertos() : 0,
    contarAguardando(),
  ]);
  const contadores = { alertas, 'busca-ativa': casos, notificacoes: aguardando };

  const menu = MENU[usuario.perfil].map(([grupo, itens]) => `
    <div class="nav-group">${grupo}</div>
    ${itens.map(([caminho, rotulo, ic]) => {
      const n = contadores[caminho];
      const atual = ativo === caminho;
      return `<a href="#/${caminho}" class="nav-item ${atual ? 'is-active' : ''}" ${atual ? 'aria-current="page"' : ''}>
        ${icone(ic)}<span>${rotulo}</span>
        ${n ? `<span class="count ${caminho === 'alertas' ? '' : 'soft'}" aria-label="${n} itens">${n}</span>` : ''}
      </a>`;
    }).join('')}`).join('');

  return `<div class="shell">
    <aside class="side" aria-label="Navegação">
      <div class="brand">${logo(40)}<div><strong>EduAcompanha</strong><span>${usuario.perfil === 'admin' ? 'Administração · Equipe Nitrix' : 'Secretaria escolar'}</span></div></div>
      ${escola ? `<div class="side-school"><span class="label">Escola</span>${esc(escola.nome)}<span class="mono">CIE ${esc(escola.cie)}</span></div>` : ''}
      <nav aria-label="Menu principal">${menu}</nav>
      <div class="side-foot">
        <span class="avatar" aria-hidden="true">${iniciais(usuario.nome)}</span>
        <div class="who"><b>${esc(usuario.nome)}</b><span>${usuario.perfil === 'admin' ? 'Administrador' : 'Secretaria'}</span></div>
        ${botaoTema()}
        <button class="icon-btn" type="button" data-acao="sessao:sair" aria-label="Sair" title="Sair">${icone('logout')}</button>
      </div>
    </aside>
    <div class="scrim" data-acao="menu:fechar"></div>
    <div class="main">
      <header class="topbar">
        <button class="icon-btn" type="button" data-acao="menu:alternar" aria-label="Abrir menu">${icone('menu')}</button>
        ${logo(28)}<strong>EduAcompanha</strong>
        ${botaoTema('no-topo')}
      </header>
      <div class="demo-bar">${icone('info', 16)}
        <span><b>Modo demonstração.</b> Ainda sem servidor: os dados ficam salvos só neste navegador e as mensagens de WhatsApp são simuladas.</span>
        <button class="btn-link" type="button" data-acao="demo:restaurar">Restaurar dados de exemplo</button>
      </div>
      <main id="content" class="content" tabindex="-1">${conteudo}</main>
    </div>
  </div>`;
}

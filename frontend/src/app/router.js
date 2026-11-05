// Rotas por hash (#/alunos/alu_1) e controle de acesso por perfil.
// Cada módulo exporta uma lista `rotas` com { caminho, perfis, tela, menu }.

import { usuarioAtual } from './session.js';
import { montarLayout, fecharMenu } from './layout.js';
import { fecharModal } from '../components/modal.js';
import { AcessoNegado, NaoEncontrado } from '../utils/validacao.js';

const rotas = [];
const telasPublicas = {};
const telasErro = {};

export const registrarRotas = (lista) => rotas.push(...lista);
export const registrarTelasPublicas = (mapa) => Object.assign(telasPublicas, mapa);
export const registrarTelasErro = (mapa) => Object.assign(telasErro, mapa);

export const caminhoAtual = () => decodeURIComponent(location.hash.replace(/^#\/?/, '')) || 'inicio';

export function navegar(caminho) {
  if (caminhoAtual() === caminho) atualizar();
  else location.hash = `#/${caminho}`;
}

let temporizador = null;
let geracao = 0;
let ultimoCaminho = null;

/** Redesenha a tela atual (com atraso opcional, para buscas enquanto digita). */
export function atualizar(atraso = 0) {
  clearTimeout(temporizador);
  temporizador = setTimeout(renderizar, atraso);
}

export function iniciarRoteador() {
  window.addEventListener('hashchange', () => { fecharModal(); fecharMenu(); renderizar(); });
  renderizar();
}

function encontrar(caminho) {
  const partes = caminho.split('/');
  for (const rota of rotas) {
    const modelo = rota.caminho.split('/');
    if (modelo.length !== partes.length) continue;
    const params = {};
    const confere = modelo.every((parte, i) => {
      if (parte.startsWith(':')) { params[parte.slice(1)] = partes[i]; return true; }
      return parte === partes[i];
    });
    if (confere) return { rota, params };
  }
  return null;
}

async function renderizar() {
  const minha = ++geracao;
  const app = document.getElementById('app');
  const caminho = caminhoAtual();
  const usuario = usuarioAtual();

  if (!usuario) {
    const tela = telasPublicas[caminho] || telasPublicas.login;
    app.innerHTML = tela();
    document.title = 'Entrar · EduAcompanha';
    ultimoCaminho = null;
    return;
  }
  if (telasPublicas[caminho]) { location.replace('#/inicio'); return; }

  const mesmaTela = caminho === ultimoCaminho;
  const ativo = document.activeElement;
  const focoId = ativo?.id && app.contains(ativo) ? ativo.id : null;
  const selecao = focoId && 'selectionStart' in ativo ? [ativo.selectionStart, ativo.selectionEnd] : null;

  // Mostra "Carregando" só se a tela demorar.
  const aviso = mesmaTela ? null : setTimeout(() => {
    const conteudo = document.getElementById('content');
    if (conteudo && minha === geracao) conteudo.innerHTML = '<div class="loading-page"><span class="spinner"></span>Carregando…</div>';
  }, 200);

  const achado = encontrar(caminho);
  let html;
  let menu = '';
  try {
    if (!achado) html = telasErro.naoEncontrada();
    else {
      menu = achado.rota.menu || achado.rota.caminho.split('/')[0];
      html = achado.rota.perfis.includes(usuario.perfil) ? await achado.rota.tela(achado.params) : telasErro.negado();
    }
  } catch (erro) {
    if (erro instanceof AcessoNegado) html = telasErro.negado();
    else if (erro instanceof NaoEncontrado) html = telasErro.naoEncontrada();
    else { console.error(erro); html = telasErro.falha(); }
  }

  let pagina;
  try { pagina = await montarLayout(html, menu); } catch (erro) { console.error(erro); pagina = html; }
  clearTimeout(aviso);
  if (minha !== geracao) return; // outra navegação começou no meio do caminho

  const rolagem = window.scrollY;
  app.innerHTML = pagina;
  window.scrollTo(0, mesmaTela ? rolagem : 0);

  if (focoId) {
    const campo = document.getElementById(focoId);
    if (campo) {
      campo.focus({ preventScroll: true });
      if (selecao && campo.setSelectionRange) { try { campo.setSelectionRange(selecao[0], selecao[1]); } catch { /* campos de data não aceitam */ } }
    }
  } else if (!mesmaTela) {
    document.getElementById('content')?.focus({ preventScroll: true });
  }

  ultimoCaminho = caminho;
  const titulo = app.querySelector('h1');
  document.title = `${titulo ? `${titulo.textContent.trim()} · ` : ''}EduAcompanha`;
}

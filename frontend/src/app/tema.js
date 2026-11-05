// Tema da interface: segue o sistema, claro ou escuro. A escolha fica salva neste navegador.
// O index.html aplica o tema salvo antes de carregar o CSS, para a tela não piscar.

import { icone } from '../components/icons.js';

const CHAVE = 'eduacompanha:tema';

/** Ordem do ciclo: [valor, rótulo, ícone] */
const TEMAS = [
  ['sistema', 'Padrão do sistema', 'monitor'],
  ['claro', 'Claro', 'sun'],
  ['escuro', 'Escuro', 'moon'],
];

function temaAtual() {
  try {
    const salvo = localStorage.getItem(CHAVE);
    if (TEMAS.some(([valor]) => valor === salvo)) return salvo;
  } catch { /* sem armazenamento: usa o padrão */ }
  return 'sistema';
}

function aplicar(tema) {
  const raiz = document.documentElement;
  if (tema === 'sistema') raiz.removeAttribute('data-theme');
  else raiz.dataset.theme = tema === 'escuro' ? 'dark' : 'light';
}

function rotuloBotao(tema) {
  const [, rotulo] = TEMAS.find(([valor]) => valor === tema);
  return `Tema: ${rotulo}. Clique para trocar`;
}

/** Botão que alterna o tema. Use a classe extra para ajustar a posição. */
export function botaoTema(classe = '') {
  const tema = temaAtual();
  const [, , ic] = TEMAS.find(([valor]) => valor === tema);
  const rotulo = rotuloBotao(tema);
  return `<button class="icon-btn btn-tema ${classe}" type="button" data-acao="tema:alternar" aria-label="${rotulo}" title="${rotulo}">${icone(ic)}</button>`;
}

export function alternarTema() {
  const indice = TEMAS.findIndex(([valor]) => valor === temaAtual());
  const [proximo, , ic] = TEMAS[(indice + 1) % TEMAS.length];
  try { localStorage.setItem(CHAVE, proximo); } catch { /* vale só nesta sessão */ }
  aplicar(proximo);

  const rotulo = rotuloBotao(proximo);
  for (const botao of document.querySelectorAll('[data-acao="tema:alternar"]')) {
    botao.innerHTML = icone(ic);
    botao.setAttribute('aria-label', rotulo);
    botao.title = rotulo;
  }
}

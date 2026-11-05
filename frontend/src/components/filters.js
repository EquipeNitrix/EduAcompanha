// Filtros de listagem. Todos usam data-ui="tela.campo": ao mudar, a tela é redesenhada.

import { icone } from './icons.js';
import { esc } from '../utils/texto.js';

const idDe = (chave) => `flt_${chave.replace(/[^a-z0-9]/gi, '_')}`;

export function filtroSelect({ rotulo, chave, valor, opcoes, todas = 'Todas', limpa = '', largo = false }) {
  const id = idDe(chave);
  return `<div class="filter ${largo ? 'grow' : ''}" ${largo ? 'style="max-width:460px"' : ''}>
    <label for="${id}">${rotulo}</label>
    <select id="${id}" data-ui="${chave}" ${limpa ? `data-ui-limpa="${limpa}"` : ''}>
      ${todas !== null ? `<option value="">${esc(todas)}</option>` : ''}
      ${opcoes.map(([v, t]) => `<option value="${esc(v)}" ${String(v) === String(valor) ? 'selected' : ''}>${esc(t)}</option>`).join('')}
    </select>
  </div>`;
}

export function filtroBusca({ rotulo = 'Buscar', chave, valor, placeholder = 'Nome' }) {
  const id = idDe(chave);
  return `<div class="filter grow">
    <label for="${id}">${rotulo}</label>
    <div class="search-input">${icone('search')}<input id="${id}" type="search" placeholder="${esc(placeholder)}" value="${esc(valor)}" data-ui="${chave}"></div>
  </div>`;
}

export function filtroData({ rotulo, chave, valor, min = '', max = '' }) {
  const id = idDe(chave);
  return `<div class="filter">
    <label for="${id}">${rotulo}</label>
    <input id="${id}" type="date" value="${esc(valor)}" ${min ? `min="${min}"` : ''} ${max ? `max="${max}"` : ''} data-ui="${chave}">
  </div>`;
}

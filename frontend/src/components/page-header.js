// Cabeçalho padrão das telas: trilha, título, explicação curta e botões.
// `titulo` aceita HTML: escape dados digitados antes de passar.

import { icone } from './icons.js';
import { esc } from '../utils/texto.js';

export function cabecalhoPagina({ trilha, sobretitulo, titulo, subtitulo, acoes }) {
  const migalhas = trilha
    ? `<nav class="crumbs" aria-label="Você está em">${trilha.map(([texto, link]) => (link
      ? `<a href="#/${link}">${esc(texto)}</a>${icone('chev', 13)}`
      : `<span aria-current="page">${esc(texto)}</span>`)).join('')}</nav>`
    : '';
  return `${migalhas}
  <div class="page-head">
    <div>
      ${sobretitulo ? `<div class="eyebrow">${sobretitulo}</div>` : ''}
      <h1>${titulo}</h1>
      ${subtitulo ? `<p class="sub">${subtitulo}</p>` : ''}
    </div>
    ${acoes ? `<div class="actions">${acoes}</div>` : ''}
  </div>`;
}

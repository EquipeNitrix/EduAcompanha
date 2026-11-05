// Mensagem para listas vazias, sempre dizendo o próximo passo.

import { icone } from './icons.js';

export function estadoVazio({ icone: ic = 'info', titulo, texto, acoes = '' }) {
  return `<div class="empty">
    <div class="em-ico">${icone(ic)}</div>
    <h3>${titulo}</h3>
    <p>${texto}</p>
    ${acoes ? `<div class="acoes">${acoes}</div>` : ''}
  </div>`;
}

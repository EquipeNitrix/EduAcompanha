// Abas. A aba escolhida fica guardada no estado da interface.
// lista: [[valor, texto, contador?]]

import { estado } from '../app/estado.js';

export function abas(chave, padrao, lista) {
  const valores = lista.map(([v]) => v);
  const salvo = estado('abas')[chave];
  const atual = valores.includes(salvo) ? salvo : padrao;
  const html = `<div class="tabs" role="tablist">${lista.map(([valor, texto, n]) => `
    <button type="button" role="tab" aria-selected="${valor === atual}" data-acao="abas:selecionar" data-chave="${chave}" data-valor="${valor}">
      ${texto}${n != null ? `<span class="n">${n}</span>` : ''}
    </button>`).join('')}</div>`;
  return { atual, html };
}

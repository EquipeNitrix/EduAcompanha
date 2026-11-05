// Avisos rápidos no canto da tela. tom: 'ok' | 'crit' | 'info'
// acao opcional: { rotulo: 'Desfazer', executar: () => {...} }

import { icone } from './icons.js';
import { esc } from '../utils/texto.js';

export function toast(mensagem, tom = 'ok', acao = null) {
  const area = document.getElementById('toasts');
  if (!area) return;
  const el = document.createElement('div');
  el.className = `toast ${tom}`;
  const ic = tom === 'crit' ? 'alert' : tom === 'info' ? 'msg' : 'ok';
  el.innerHTML = `${icone(ic)}<div class="grow">${esc(mensagem)}</div>${acao ? `<button type="button">${esc(acao.rotulo)}</button>` : ''}`;
  if (acao) {
    el.querySelector('button').addEventListener('click', () => { el.remove(); acao.executar(); });
  }
  area.appendChild(el);
  setTimeout(() => el.remove(), acao ? 8000 : 4500);
}

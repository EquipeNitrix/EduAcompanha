// Janela modal com foco preso dentro dela, Esc para fechar e clique fora para fechar.

import { icone } from './icons.js';

let focoAnterior = null;
const raiz = () => document.getElementById('modal-root');

export const modalAberto = () => Boolean(raiz()?.firstElementChild);

export function abrirModal(titulo, corpo, { largo = false } = {}) {
  focoAnterior = document.activeElement;
  raiz().innerHTML = `<div class="overlay">
    <div class="modal ${largo ? 'wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
      <header class="modal-h">
        <h2 id="modal-titulo">${titulo}</h2>
        <button class="icon-btn" type="button" data-acao="modal:fechar" aria-label="Fechar">${icone('x')}</button>
      </header>
      <div class="modal-b">${corpo}</div>
    </div>
  </div>`;
  document.body.classList.add('modal-aberto');
  setTimeout(() => {
    const primeiro = raiz().querySelector('.modal-b input:not([type=hidden]):not([readonly]), .modal-b select, .modal-b textarea, .modal-b .btn');
    primeiro?.focus();
  }, 30);
}

export function fecharModal() {
  if (!raiz()) return;
  raiz().innerHTML = '';
  document.body.classList.remove('modal-aberto');
  if (focoAnterior && document.contains(focoAnterior)) focoAnterior.focus();
  focoAnterior = null;
}

document.addEventListener('keydown', (evento) => {
  if (!modalAberto()) return;
  if (evento.key === 'Escape') { fecharModal(); return; }
  if (evento.key !== 'Tab') return;
  const focaveis = [...raiz().querySelectorAll('button:not([disabled]), [href], input:not([type=hidden]):not([disabled]), select:not([disabled]), textarea:not([disabled])')]
    .filter((el) => el.offsetParent !== null);
  if (!focaveis.length) return;
  const primeiro = focaveis[0];
  const ultimo = focaveis[focaveis.length - 1];
  if (evento.shiftKey && document.activeElement === primeiro) { evento.preventDefault(); ultimo.focus(); }
  else if (!evento.shiftKey && document.activeElement === ultimo) { evento.preventDefault(); primeiro.focus(); }
});

document.addEventListener('mousedown', (evento) => {
  if (evento.target.classList?.contains('overlay')) fecharModal();
});

// Confirmação antes de ações destrutivas ou importantes.
// Uso: confirmar({ titulo, texto, botao: 'Excluir', aoConfirmar: async () => {...} })

import { abrirModal, fecharModal } from './modal.js';
import { botaoCarregando } from './form.js';

let pendente = null;

export function confirmar({ titulo, texto, botao = 'Confirmar', tom = 'danger', aoConfirmar }) {
  pendente = aoConfirmar;
  abrirModal(titulo, `
    <div>${texto}</div>
    <div class="modal-actions">
      <button type="button" class="btn btn-ghost" data-acao="modal:fechar">Voltar</button>
      <button type="button" class="btn btn-${tom}" data-acao="modal:confirmar">${botao}</button>
    </div>`);
}

/** Chamado pela ação "modal:confirmar" (registrada em app/acoes-gerais.js). */
export async function executarConfirmacao(botao) {
  const acao = pendente;
  pendente = null;
  botaoCarregando(botao, true);
  try {
    await acao?.();
  } finally {
    fecharModal();
  }
}

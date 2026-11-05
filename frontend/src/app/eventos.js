// Eventos da página inteira (delegação).
//
// Botões e links usam data-acao="nome:da-acao" e os módulos registram o que
// cada ação faz com registrarAcoes(). Formulários usam data-form="nome" e são
// registrados com registrarFormularios(). Campos com data-ui="tela.campo"
// atualizam o estado da tela e redesenham.

import { atualizar } from './router.js';
import { definirUI } from './estado.js';
import { toast } from '../components/toast.js';
import { fecharModal } from '../components/modal.js';
import { mostrarErros, limparErros, botaoCarregando, dadosDoForm, aplicarMascara } from '../components/form.js';
import { ErroValidacao, RegraNegocio, AcessoNegado, NaoEncontrado } from '../utils/validacao.js';

const acoes = {};
const formularios = {};

export const registrarAcoes = (mapa) => Object.assign(acoes, mapa);
export const registrarFormularios = (mapa) => Object.assign(formularios, mapa);

/** Mostra o erro do jeito certo: no formulário, se houver, ou em um aviso. */
export function tratarErro(erro, form) {
  if (erro instanceof ErroValidacao) {
    if (form) mostrarErros(form, erro.campos);
    else toast(Object.values(erro.campos)[0], 'crit');
    return;
  }
  if (erro instanceof RegraNegocio) {
    if (form) mostrarErros(form, { _: erro.message });
    else toast(erro.message, 'crit');
    return;
  }
  if (erro instanceof AcessoNegado || erro instanceof NaoEncontrado) {
    fecharModal();
    toast(erro.message, 'crit');
    atualizar();
    return;
  }
  console.error(erro);
  toast('Não foi possível concluir a ação. Tente novamente.', 'crit');
}

export function iniciarEventos() {
  document.addEventListener('click', async (evento) => {
    const alvo = evento.target.closest('[data-acao]');
    if (!alvo || alvo.disabled) return;
    const acao = acoes[alvo.dataset.acao];
    if (!acao) { console.warn('Ação não registrada:', alvo.dataset.acao); return; }
    if (alvo.tagName !== 'A') evento.preventDefault();
    try {
      await acao(alvo.dataset, alvo, evento);
    } catch (erro) {
      if (document.contains(alvo)) botaoCarregando(alvo, false);
      tratarErro(erro);
    }
  });

  document.addEventListener('submit', async (evento) => {
    const form = evento.target.closest('form[data-form]');
    if (!form) return;
    evento.preventDefault();
    const enviar = formularios[form.dataset.form];
    if (!enviar) { console.warn('Formulário não registrado:', form.dataset.form); return; }
    limparErros(form);
    const botao = form.querySelector('[type="submit"]');
    botaoCarregando(botao, true);
    try {
      await enviar(dadosDoForm(form), form);
    } catch (erro) {
      tratarErro(erro, form);
    } finally {
      if (botao && document.contains(botao)) botaoCarregando(botao, false);
    }
  });

  document.addEventListener('input', (evento) => {
    const campo = evento.target;
    if (campo.dataset.mascara === 'telefone') aplicarMascara(campo);
    if (campo.dataset.mostraGrupo) {
      // Mostra só os blocos do formulário com data-grupo="<nome>" e data-grupo-valor igual ao escolhido.
      const form = campo.closest('form') || document;
      form.querySelectorAll(`[data-grupo="${campo.dataset.mostraGrupo}"]`).forEach((bloco) => { bloco.hidden = bloco.dataset.grupoValor !== campo.value; });
    }
    if (campo.dataset.alternaAlvo) {
      const bloco = document.querySelector(campo.dataset.alternaAlvo);
      if (bloco) bloco.hidden = campo.value !== campo.dataset.alternaValor;
    }
    if (campo.dataset.ui) {
      definirUI(campo.dataset.ui, campo.value);
      if (campo.dataset.uiLimpa) for (const outro of campo.dataset.uiLimpa.split(',')) definirUI(outro, '');
      atualizar(campo.type === 'search' ? 250 : 0);
    }
  });
}

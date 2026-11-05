// Campos de formulário padronizados, máscara de telefone e exibição de erros.

import { icone } from './icons.js';
import { esc } from '../utils/texto.js';
import { formatarTelefone } from '../utils/formatos.js';

export const ATRIBUTOS_TELEFONE = 'inputmode="numeric" autocomplete="off" placeholder="(15) 99999-9999" data-mascara="telefone" maxlength="15"';

/**
 * Monta um campo com rótulo, dica e espaço para erro.
 * opcoes: { rotulo, nome, tipo, valor, obrigatorio, dica, largo, opcoes: [[valor, texto]], vazio, atributos, prefixo, linhas }
 */
export function campo(o) {
  const id = `${o.prefixo || 'f'}_${o.nome}`;
  const obrigatorio = o.obrigatorio ? ' required aria-required="true"' : '';
  const extras = o.atributos || '';
  let controle;
  if (o.tipo === 'select') {
    const vazio = o.vazio !== undefined ? `<option value="">${esc(o.vazio)}</option>` : '';
    const itens = o.opcoes.map(([valor, texto]) => `<option value="${esc(valor)}" ${String(valor) === String(o.valor ?? '') ? 'selected' : ''}>${esc(texto)}</option>`).join('');
    controle = `<select id="${id}" name="${o.nome}"${obrigatorio} ${extras}>${vazio}${itens}</select>`;
  } else if (o.tipo === 'textarea') {
    controle = `<textarea id="${id}" name="${o.nome}" rows="${o.linhas || 3}"${obrigatorio} ${extras}>${esc(o.valor)}</textarea>`;
  } else if (o.tipo === 'password') {
    controle = `<div class="pwd"><input id="${id}" name="${o.nome}" type="password" value="${esc(o.valor)}"${obrigatorio} ${extras}>
      <button type="button" class="icon-btn" data-acao="senha:alternar" data-campo="${id}" aria-label="Mostrar senha">${icone('eye')}</button></div>`;
  } else {
    controle = `<input id="${id}" name="${o.nome}" type="${o.tipo || 'text'}" value="${esc(o.valor)}"${obrigatorio} ${extras}>`;
  }
  return `<div class="field ${o.largo ? 'span-2' : ''}">
    <label for="${id}">${esc(o.rotulo)}${o.obrigatorio ? '<span class="req" aria-hidden="true">*</span>' : ''}</label>
    ${controle}
    ${o.dica ? `<p class="hint">${o.dica}</p>` : ''}
  </div>`;
}

/** Rodapé padrão de formulário em modal. */
export function botoesModal(textoSalvar, { tom = 'primary', largo = true } = {}) {
  return `<div class="modal-actions ${largo ? 'span-2' : ''}">
    <button type="button" class="btn btn-ghost" data-acao="modal:fechar">Cancelar</button>
    <button class="btn btn-${tom}" type="submit">${textoSalvar}</button>
  </div>`;
}

export function aplicarMascara(input) {
  input.value = formatarTelefone(input.value);
}

export const dadosDoForm = (form) => Object.fromEntries(new FormData(form));

export function botaoCarregando(botao, carregando) {
  if (!botao) return;
  botao.classList.toggle('is-loading', carregando);
  botao.disabled = carregando;
}

export function limparErros(form) {
  form.querySelectorAll('.err').forEach((e) => e.remove());
  form.querySelectorAll('[aria-invalid]').forEach((e) => { e.removeAttribute('aria-invalid'); e.removeAttribute('aria-describedby'); });
  form.querySelectorAll('.form-alert').forEach((a) => a.remove());
}

/** Mostra { campo: mensagem } abaixo de cada campo. A chave "_" vira um aviso no topo. */
export function mostrarErros(form, erros) {
  let primeiro = null;
  for (const [nome, mensagem] of Object.entries(erros)) {
    const controle = nome === '_' ? null : form.querySelector(`[name="${nome}"]`);
    if (!controle) {
      const alerta = document.createElement('div');
      alerta.className = 'form-alert span-2';
      alerta.setAttribute('role', 'alert');
      alerta.tabIndex = -1;
      alerta.innerHTML = `${icone('alert')}<span>${esc(mensagem)}</span>`;
      form.prepend(alerta);
      primeiro ||= alerta;
      continue;
    }
    controle.setAttribute('aria-invalid', 'true');
    const p = document.createElement('p');
    p.className = 'err';
    p.id = `${controle.id}_erro`;
    p.innerHTML = `${icone('alert', 14)}<span>${esc(mensagem)}</span>`;
    controle.setAttribute('aria-describedby', p.id);
    controle.closest('.field').appendChild(p);
    primeiro ||= controle;
  }
  primeiro?.focus();
}

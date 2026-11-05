// Exportação de relatórios: planilha (.csv, abre no Excel) e PDF (pela impressão do navegador).
// O arquivo reflete os filtros aplicados na tela no momento da exportação.

import { abrirModal, fecharModal } from './modal.js';
import { icone } from './icons.js';
import { logo } from './logo.js';
import { esc } from '../utils/texto.js';
import { fmtDataHora } from '../utils/datas.js';
import { usuarioAtual } from '../app/session.js';

let atual = null;

/** Abre a prévia com os botões de baixar. */
export function abrirExportacao({ titulo, filtros, colunas, linhas, nomeArquivo }) {
  atual = { titulo, filtros, colunas, linhas, nomeArquivo };
  abrirModal('Exportar relatório', `
    <p class="small muted">${linhas.length} ${linhas.length === 1 ? 'linha' : 'linhas'} com os filtros atuais. Para salvar em PDF, escolha "Salvar como PDF" na janela de impressão.</p>
    <div class="preview">${relatorioHTML(atual)}</div>
    <div class="modal-actions">
      <button type="button" class="btn btn-ghost" data-acao="modal:fechar">Fechar</button>
      <button type="button" class="btn btn-ghost" data-acao="exportar:csv">${icone('download')}Baixar planilha (.csv)</button>
      <button type="button" class="btn btn-primary" data-acao="exportar:pdf">${icone('print')}Gerar PDF</button>
    </div>`, { largo: true });
}

function relatorioHTML({ titulo, filtros, colunas, linhas }) {
  return `<div class="relatorio">
    <header>${logo(34)}<div><h1>${esc(titulo)}</h1><div class="filtros">${esc(filtros)}</div></div></header>
    <table><thead><tr>${colunas.map((c) => `<th>${esc(c)}</th>`).join('')}</tr></thead>
    <tbody>${linhas.map((l) => `<tr>${l.map((v) => `<td>${esc(v)}</td>`).join('')}</tr>`).join('')}</tbody></table>
    <footer>Gerado em ${fmtDataHora(new Date().toISOString())} por ${esc(usuarioAtual()?.nome || '')} · EduAcompanha</footer>
  </div>`;
}

export function exportarCSVAtual() {
  if (!atual) return;
  const celula = (v) => { const s = String(v ?? ''); return /[;"\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s; };
  const conteudo = `﻿${[atual.colunas, ...atual.linhas].map((l) => l.map(celula).join(';')).join('\r\n')}`;
  const url = URL.createObjectURL(new Blob([conteudo], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `${atual.nomeArquivo}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function exportarPDFAtual() {
  if (!atual) return;
  const raiz = document.getElementById('print-root');
  raiz.innerHTML = relatorioHTML(atual);
  fecharModal();
  document.body.classList.add('imprimindo');
  const limpar = () => { document.body.classList.remove('imprimindo'); raiz.innerHTML = ''; };
  window.addEventListener('afterprint', limpar, { once: true });
  window.print();
  setTimeout(limpar, 1500);
}

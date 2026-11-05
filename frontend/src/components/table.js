// Tabela padrão com rolagem horizontal no celular.
// colunas: [{ texto, classe }] ou textos simples; linhas: HTML de cada <tr>.

export function tabela(colunas, linhas, { classe = '' } = {}) {
  const cabecalho = colunas.map((c) => (typeof c === 'string' ? `<th>${c}</th>` : `<th class="${c.classe || ''}">${c.texto}</th>`)).join('');
  return `<div class="tbl-wrap"><table class="tbl ${classe}"><thead><tr>${cabecalho}</tr></thead><tbody>${linhas.join('')}</tbody></table></div>`;
}

/** Botão "Mostrar mais" para listas longas. */
export function botaoMostrarMais(chave, total, limite, passo = 50) {
  if (total <= limite) return '';
  return `<div class="more"><button class="btn btn-ghost btn-sm" type="button" data-acao="lista:mais" data-chave="${chave}" data-inicial="${limite}" data-passo="${passo}">Mostrar mais (${total - limite} restantes)</button></div>`;
}

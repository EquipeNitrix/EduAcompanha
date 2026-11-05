// Funções de texto usadas em toda a interface.

/** Escapa texto antes de colocar dentro do HTML. Use sempre em dados digitados pelo usuário. */
export function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Remove acentos e deixa minúsculo, para buscas. */
export function normalizar(texto) {
  return String(texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

export function iniciais(nome) {
  const partes = String(nome || '').trim().split(/\s+/).filter(Boolean);
  if (!partes.length) return '?';
  return (partes[0][0] + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
}

export const primeiroNome = (nome) => String(nome || '').split(' ')[0];
export const plural = (n, um, varios) => `${n} ${n === 1 ? um : varios}`;
export const semPrefixoEscola = (nome) => String(nome || '').replace(/^E\.E\.\s*/, '');
export const limparEspacos = (texto) => String(texto || '').trim().replace(/\s+/g, ' ');

/** Agrupa nomes repetidos: ["Matemática", "Matemática", "Arte"] -> "Matemática (2) e Arte". */
export function resumirRepetidos(nomes) {
  const contagem = new Map();
  for (const nome of nomes) contagem.set(nome, (contagem.get(nome) || 0) + 1);
  return listaPorExtenso([...contagem].map(([nome, n]) => (n > 1 ? `${nome} (${n})` : nome)));
}

/** Junta uma lista em português: "a, b e c". */
export function listaPorExtenso(itens) {
  if (itens.length <= 1) return itens.join('');
  return `${itens.slice(0, -1).join(', ')} e ${itens[itens.length - 1]}`;
}

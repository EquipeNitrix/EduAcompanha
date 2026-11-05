// Formatação de valores exibidos na tela.

export const fmtPct = (p) =>
  p == null ? '-' : `${p.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;

export const ordemAula = (n) => `${n}ª aula`;

/** Nome da posição do telefone na fila de contato: Principal, Secundário, Secundário 2... */
export function rotuloTelefone(indice) {
  if (indice === 0) return 'Principal';
  if (indice === 1) return 'Secundário';
  return `Secundário ${indice}`;
}

/** Esconde o meio do número em listas: (15) •••••-1234 */
export function mascararTelefone(numero) {
  return String(numero || '').replace(/(\d{4,5})-(\d{4})$/, (_, meio, fim) => `${'•'.repeat(meio.length)}-${fim}`);
}

/** Formata enquanto a pessoa digita: (15) 99999-9999 ou (15) 3333-4444 */
export function formatarTelefone(valor) {
  const d = String(valor).replace(/\D/g, '').slice(0, 11);
  if (!d) return '';
  if (d.length <= 2) return `(${d}`;
  const corte = d.length > 10 ? 7 : 6;
  if (d.length <= corte) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, corte)}-${d.slice(corte)}`;
}

export const somenteDigitos = (texto) => String(texto || '').replace(/\D/g, '');

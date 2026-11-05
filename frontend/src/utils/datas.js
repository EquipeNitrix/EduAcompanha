// Funções de data. Todas as datas do sistema circulam como texto ISO "AAAA-MM-DD",
// o que permite comparar com < e > sem converter.

export const DIAS_SEMANA = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];
const DIAS_PLURAL = { 1: 'segundas-feiras', 2: 'terças-feiras', 3: 'quartas-feiras', 4: 'quintas-feiras', 5: 'sextas-feiras' };
const DIAS_ARTIGO = { 1: 'às segundas', 2: 'às terças', 3: 'às quartas', 4: 'às quintas', 5: 'às sextas' };

export const pad = (n) => String(n).padStart(2, '0');

export function paraISO(data) {
  return `${data.getFullYear()}-${pad(data.getMonth() + 1)}-${pad(data.getDate())}`;
}

export function deISO(texto) {
  const [ano, mes, dia] = texto.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

export function somarDias(texto, dias) {
  const d = deISO(texto);
  d.setDate(d.getDate() + dias);
  return paraISO(d);
}

/** 0 = domingo ... 6 = sábado */
export const diaSemana = (texto) => deISO(texto).getDay();
export const ehDiaUtil = (texto) => { const w = diaSemana(texto); return w >= 1 && w <= 5; };
export const hoje = () => paraISO(new Date());
export const horaAgora = () => { const d = new Date(); return `${pad(d.getHours())}:${pad(d.getMinutes())}`; };
export const diasEntre = (de, ate) => Math.round((deISO(ate) - deISO(de)) / 864e5);

/** Lista os dias de segunda a sexta entre duas datas (inclusive). */
export function diasUteis(de, ate) {
  const lista = [];
  for (let d = de; d <= ate; d = somarDias(d, 1)) if (ehDiaUtil(d)) lista.push(d);
  return lista;
}

/** Próximo dia útil a partir de uma data (inclusive). */
export function proximoDiaUtil(texto) {
  let d = texto;
  while (!ehDiaUtil(d)) d = somarDias(d, 1);
  return d;
}

/** Último dia útil até uma data (inclusive). */
export function ultimoDiaUtil(texto) {
  let d = texto;
  while (!ehDiaUtil(d)) d = somarDias(d, -1);
  return d;
}

export const fmtData = (texto) => (texto ? `${texto.slice(8, 10)}/${texto.slice(5, 7)}/${texto.slice(0, 4)}` : '-');

export function fmtDataHora(valor) {
  if (!valor) return '-';
  const d = new Date(valor);
  return `${fmtData(paraISO(d))} às ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export const fmtHora = (valor) => { const d = new Date(valor); return `${pad(d.getHours())}:${pad(d.getMinutes())}`; };
export const nomeDiaPlural = (w) => DIAS_PLURAL[w] || DIAS_SEMANA[w];
export const nomeDiaComArtigo = (w) => DIAS_ARTIGO[w] || DIAS_SEMANA[w];
export const dataPorExtenso = () => new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });

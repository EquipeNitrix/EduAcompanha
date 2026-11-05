// "Banco de dados" de demonstração, salvo no localStorage do navegador.
// Só o data/api.js deve importar este arquivo.

import { gerarDadosIniciais } from './seed.js';

const CHAVE = 'eduacompanha.mock.v7';
const VERSAO = 7;
let banco = null;
let sequencia = 0;

export function carregar() {
  try {
    const bruto = localStorage.getItem(CHAVE);
    if (bruto) {
      const salvo = JSON.parse(bruto);
      if (salvo?.versao === VERSAO) { banco = salvo; return; }
    }
  } catch { /* armazenamento indisponível: segue em memória */ }
  banco = gerarDadosIniciais();
  salvar();
}

export function restaurar() {
  banco = gerarDadosIniciais();
  salvar();
}

function salvar() {
  try { localStorage.setItem(CHAVE, JSON.stringify(banco)); } catch { /* segue em memória */ }
}

const novoId = (prefixo) => `${prefixo}_${Date.now().toString(36)}${(sequencia++).toString(36)}${Math.random().toString(36).slice(2, 5)}`;

function colecao(nome) {
  const c = banco.colecoes[nome];
  if (!c) throw new Error(`Coleção desconhecida: ${nome}`);
  return c;
}

/**
 * Filtros por igualdade. Um valor em lista significa "qualquer um destes".
 * `dataDe` e `dataAte` filtram o campo `data` por intervalo.
 */
function confere(item, filtros) {
  for (const [campo, valor] of Object.entries(filtros)) {
    if (valor === undefined) continue;
    if (campo === 'dataDe') { if (item.data < valor) return false; continue; }
    if (campo === 'dataAte') { if (item.data > valor) return false; continue; }
    if (Array.isArray(valor)) { if (!valor.includes(item[campo])) return false; continue; }
    if (item[campo] !== valor) return false;
  }
  return true;
}

const copia = (item) => (item ? JSON.parse(JSON.stringify(item)) : null);

export const listar = (nome, filtros = {}) => colecao(nome).filter((i) => confere(i, filtros)).map(copia);
export const obter = (nome, id) => copia(colecao(nome).find((i) => i.id === id));
export const encontrar = (nome, condicao) => copia(colecao(nome).find(condicao));

export function criar(nome, dados, prefixo) {
  const item = { ...copia(dados), id: novoId(prefixo) };
  colecao(nome).push(item);
  salvar();
  return copia(item);
}

export function atualizar(nome, id, alteracoes) {
  const item = colecao(nome).find((i) => i.id === id);
  if (!item) throw new Error(`Registro não encontrado: ${nome}/${id}`);
  Object.assign(item, copia(alteracoes), { id });
  salvar();
  return copia(item);
}

export function remover(nome, id) {
  const lista = colecao(nome);
  const indice = lista.findIndex((i) => i.id === id);
  if (indice >= 0) lista.splice(indice, 1);
  salvar();
}

export const configuracao = () => copia(banco.config);

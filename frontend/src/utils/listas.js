// Ajudantes para trabalhar com listas de registros.

/** Agrupa em um Map: chave -> lista. `chave` pode ser o nome do campo ou uma função. */
export function agruparPor(lista, chave) {
  const mapa = new Map();
  for (const item of lista) {
    const k = typeof chave === 'function' ? chave(item) : item[chave];
    if (!mapa.has(k)) mapa.set(k, []);
    mapa.get(k).push(item);
  }
  return mapa;
}

/** Map de id -> registro. */
export const indexarPor = (lista, chave = 'id') => new Map(lista.map((item) => [item[chave], item]));

export const unicos = (lista) => [...new Set(lista)];

export const porNome = (a, b) => a.nome.localeCompare(b.nome, 'pt-BR');

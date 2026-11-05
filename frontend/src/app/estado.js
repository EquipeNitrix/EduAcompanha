// Estado da interface (filtros, aba selecionada, data escolhida...).
// Fica em memória enquanto a pessoa usa o sistema e é limpo ao sair.

const ui = {};

/** Devolve o objeto de estado de uma tela, criando com os valores iniciais se não existir. */
export function estado(chave, inicial = {}) {
  if (!ui[chave]) ui[chave] = { ...inicial };
  return ui[chave];
}

/** Altera um campo usando "chave.campo", por exemplo "alunos.q". */
export function definirUI(caminho, valor) {
  const ponto = caminho.indexOf('.');
  const chave = caminho.slice(0, ponto);
  const campo = caminho.slice(ponto + 1);
  (ui[chave] ||= {})[campo] = valor;
}

export function limparEstado() {
  for (const chave of Object.keys(ui)) delete ui[chave];
}

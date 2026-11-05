// Etiqueta de estado. tom: '' | 'ok' | 'warn' | 'crit' | 'info' | 'sun' | 'muted'
// O texto pode conter HTML: escape dados digitados antes de passar.

export const chip = (texto, tom = '', { semPonto = false } = {}) =>
  `<span class="chip ${tom} ${semPonto ? 'plain' : ''}">${texto}</span>`;

// Barra de frequência com a marca dos 75%.

import { LIMITE_FREQUENCIA, LIMITE_ATENCAO } from '../app/constantes.js';
import { fmtPct } from '../utils/formatos.js';

export function tomFrequencia(pct) {
  if (pct == null) return '';
  if (pct < LIMITE_FREQUENCIA) return 'crit';
  if (pct < LIMITE_ATENCAO) return 'warn';
  return '';
}

export function medidorFrequencia(freq) {
  if (!freq || freq.pct == null) return '<span class="muted small">Sem aulas previstas</span>';
  const tom = tomFrequencia(freq.pct);
  return `<span class="pct ${tom}">
    <b>${fmtPct(freq.pct)}</b>
    <span class="meter ${tom}" aria-hidden="true"><i style="width:${Math.min(100, freq.pct)}%"></i><span class="mark"></span></span>
  </span>`;
}

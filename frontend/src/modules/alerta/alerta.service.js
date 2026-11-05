// Alerta de evasão e padrões de falta.
//
// - Um aluno entra no alerta quando a frequência do ano letivo fica abaixo de 75%.
//   A entrada é automática; abrir um caso de Busca Ativa é sempre decisão da secretaria.
// - Padrão de falta (últimos 30 dias): 3 ou mais faltas no mesmo dia da semana.
//   O limite de 3 em 30 dias é uma referência proposta, a validar com a equipe.

import { api } from '../../data/api.js';
import { escolaDoUsuario } from '../../app/session.js';
import { PADRAO_JANELA_DIAS, PADRAO_MINIMO, LIMITE_FREQUENCIA, LIMITE_ATENCAO } from '../../app/constantes.js';
import { somarDias, diaSemana, hoje, nomeDiaPlural } from '../../utils/datas.js';
import { agruparPor, indexarPor } from '../../utils/listas.js';
import { plural } from '../../utils/texto.js';
import { carregarBase, frequenciaDe } from '../frequencia/frequencia.service.js';
import { aulasDaSemana, aulasNaData } from '../configuracao/configuracao.service.js';

/** Todos os alunos com matrícula ativa no ano, com a frequência calculada. */
export async function indicadores({ escola = '', turma = '' } = {}) {
  const escolaId = escolaDoUsuario() || escola || undefined;
  const base = await carregarBase({ escolaId });
  const matriculas = (await api.matriculas.listar({ ano: base.cfg.anoAtual, status: 'ativa' }))
    .filter((m) => base.turmaPorId.has(m.turmaId) && (!turma || m.turmaId === turma));
  const alunos = indexarPor(await api.alunos.listar({ id: matriculas.map((m) => m.alunoId) }));
  return matriculas.map((m) => ({ matricula: m, turma: base.turmaPorId.get(m.turmaId), aluno: alunos.get(m.alunoId), freq: frequenciaDe(m, base) }));
}

/** Nível de atenção na tela de Alerta de Evasão: 'critico' já está abaixo do mínimo da LDB;
 * 'atencao' é a faixa preventiva (entre LIMITE_FREQUENCIA e LIMITE_ATENCAO), para agir antes
 * de o aluno ficar crítico. */
export function nivelAlerta(pct) {
  if (pct == null) return null;
  if (pct < LIMITE_FREQUENCIA) return 'critico';
  if (pct < LIMITE_ATENCAO) return 'atencao';
  return null;
}

export async function alunosEmAlerta(filtros = {}) {
  return (await indicadores(filtros))
    .map((x) => ({ ...x, nivel: nivelAlerta(x.freq.pct) }))
    .filter((x) => x.nivel)
    .sort((a, b) => a.freq.pct - b.freq.pct);
}

export async function contarAlertas() {
  return (await alunosEmAlerta()).length;
}

export async function padroesDeFalta(alunoId) {
  const cfg = await api.configuracao();
  const [matricula] = await api.matriculas.listar({ alunoId, ano: cfg.anoAtual, status: 'ativa' });
  if (!matricula) return [];
  const anoLetivo = cfg.anosLetivos[matricula.ano];
  const fim = hoje() < anoLetivo.fim ? hoje() : anoLetivo.fim;
  let inicio = somarDias(fim, -(PADRAO_JANELA_DIAS - 1));
  if (matricula.inicio > inicio) inicio = matricula.inicio;

  const faltas = await api.lancamentos.listar({ matriculaId: matricula.id, tipo: 'falta', status: 'ativo', dataDe: inicio, dataAte: fim });
  if (faltas.length < PADRAO_MINIMO) return [];
  const turma = await api.turmas.obter(matricula.turmaId);
  const semana = await aulasDaSemana(turma.escolaId, turma.turno);

  const padroes = [];
  for (const [w, lista] of agruparPor(faltas, (l) => diaSemana(l.data))) {
    if (lista.length < PADRAO_MINIMO) continue;
    const aulas = lista.reduce((soma, l) => soma + (aulasNaData(semana, l.data) || 0), 0);
    padroes.push({
      tipo: 'dia',
      titulo: `Faltou em ${lista.length} ${nomeDiaPlural(w)} nos últimos ${PADRAO_JANELA_DIAS} dias`,
      detalhe: semana ? `${plural(aulas, 'aula perdida', 'aulas perdidas')} nesses dias.` : 'As faltas se repetem no mesmo dia da semana.',
    });
  }
  return padroes;
}

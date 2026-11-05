// Configuração de cada turno da escola: horário de envio das mensagens de falta
// e horários das aulas de cada dia da semana.
//
// Regras:
// - A escola tem um modelo de turno: integral (um turno, o dia todo) ou parcial
//   (manhã, tarde e noite). Só os turnos do modelo aparecem e podem ter turmas.
// - Administrador e secretaria (da própria escola) podem configurar.
// - O horário de envio é opcional. Turno sem horário não envia mensagens.
// - A grade de horários diz quantas aulas há em cada dia (o número de linhas)
//   e quando cada uma começa e termina. Intervalo, almoço e tutoria não entram.
// - Dia sem nenhuma aula = sem aula. Turno sem grade: não dá para lançar
//   ocorrências nem calcular a frequência das turmas dele.
// - Cada mudança na grade tem data de vigência: os dias anteriores continuam
//   com a grade antiga, então a frequência já calculada não muda.
//
// Aulas perdidas (regra da chamada: a aula conta como presente se o aluno
// estava na escola no início dela):
// - falta: todas as aulas do dia;
// - atraso: as que começaram antes da hora de entrada;
// - saída antecipada: as que começam a partir da hora de saída;
// - saiu e voltou: as que começam entre a saída e a volta.
//
// No banco: modelo e horários de envio em ESCOLA; grade em HORARIO_AULA.

import { api } from '../../data/api.js';
import { exigirEscola } from '../../app/session.js';
import { TURNO_EXTENSO, turnosDaEscola } from '../../app/constantes.js';
import { diaSemana, hoje } from '../../utils/datas.js';
import { validar, NaoEncontrado, RegraNegocio } from '../../utils/validacao.js';

export const DIAS_LETIVOS = [1, 2, 3, 4, 5];
export const MAX_AULAS_DIA = 15;
const HORA_RE = /^([01]\d|2[0-3]):[0-5]\d$/;

const chave = (escolaId, turno) => `${escolaId}:${turno}`;

// ---------- Funções puras ----------
// A grade de um turno é uma lista de VERSÕES:
// [{ vigencia: 'AAAA-MM-DD', dias: { 1: [{ ordem, inicio, fim }], ..., 5: [...] } }]
// null = turno nunca configurado.

/** Monta as versões a partir das linhas de HORARIO_AULA de um turno. */
export function versoesDe(linhas) {
  if (!linhas.length) return null;
  const datas = [...new Set(linhas.map((l) => l.vigencia))].sort();
  return datas.map((vigencia) => {
    const dias = {};
    for (const dia of DIAS_LETIVOS) {
      dias[dia] = linhas
        .filter((l) => l.vigencia === vigencia && l.dia === dia)
        .sort((a, b) => a.ordem - b.ordem)
        .map((l) => ({ ordem: l.ordem, inicio: l.inicio, fim: l.fim }));
    }
    return { vigencia, dias, aulas: DIAS_LETIVOS.map((d) => dias[d].length) };
  });
}

/** Versão em vigor numa data (a de maior vigência até a data), ou null. */
export function versaoNaData(versoes, data) {
  if (!versoes) return null;
  let vigente = null;
  for (const v of versoes) {
    if (v.vigencia <= data) vigente = v;
    else break;
  }
  return vigente;
}

/** Aulas da data, em ordem: [{ ordem, inicio, fim }]. null = turno não configurado. */
export function aulasDoDia(versoes, data) {
  if (!versoes) return null;
  const versao = versaoNaData(versoes, data);
  const w = diaSemana(data);
  if (!versao || w < 1 || w > 5) return [];
  return versao.dias[w];
}

/** Quantidade de aulas numa data. null = turno não configurado; 0 = sem aula. */
export function aulasNaData(versoes, data) {
  const aulas = aulasDoDia(versoes, data);
  return aulas === null ? null : aulas.length;
}

/**
 * Aulas que uma ocorrência faz o aluno perder, pela regra da chamada.
 * Devolve a lista de aulas perdidas ([{ ordem, inicio, fim }]) ou null se o turno não tem grade.
 */
export function aulasPerdidas(versoes, data, { tipo, horaEntrada, horaSaida }) {
  const aulas = aulasDoDia(versoes, data);
  if (aulas === null) return null;
  if (tipo === 'falta') return aulas;
  if (tipo === 'atraso') return aulas.filter((a) => a.inicio < horaEntrada);
  if (tipo === 'saida') return aulas.filter((a) => a.inicio >= horaSaida);
  if (tipo === 'saidaRetorno') return aulas.filter((a) => a.inicio >= horaSaida && a.inicio < horaEntrada);
  return [];
}

/** Aulas [seg..sex] da versão em vigor hoje (ou na data), ou null. */
export const semanaVigente = (versoes, data = hoje()) => versaoNaData(versoes, data)?.aulas || null;

export const totalSemana = (semana) => (semana ? semana.reduce((a, b) => a + b, 0) : 0);

/** Primeiro início e último fim do dia, para mostrar "07:30 às 16:30". */
export const periodoDoDia = (aulas) => (aulas?.length ? { inicio: aulas[0].inicio, fim: aulas[aulas.length - 1].fim } : null);

const mesmaGrade = (a, b) => DIAS_LETIVOS.every((d) => a[d].length === b[d].length
  && a[d].every((x, i) => x.inicio === b[d][i].inicio && x.fim === b[d][i].fim));

// ---------- Consultas ----------

/** Versões da grade de várias escolas de uma vez: obter(escolaId, turno). */
export async function mapaDeAulas(escolaId) {
  const linhas = await api.horarios.listar({ escolaId });
  const grupos = new Map();
  for (const l of linhas) {
    const k = chave(l.escolaId, l.turno);
    if (!grupos.has(k)) grupos.set(k, []);
    grupos.get(k).push(l);
  }
  const mapa = new Map();
  for (const [k, lista] of grupos) mapa.set(k, versoesDe(lista));
  return { obter: (escola, turno) => mapa.get(chave(escola, turno)) || null };
}

export async function aulasDaSemana(escolaId, turno) {
  return versoesDe(await api.horarios.listar({ escolaId, turno }));
}

/** Horário de envio das mensagens do turno ("HH:MM") ou null. */
export async function horarioEnvio(escolaId, turno) {
  const escola = await api.escolas.obter(escolaId);
  return escola?.horarioEnvio?.[turno] || null;
}

/** Os turnos do modelo da escola, com horário, versões da grade e quantas turmas do ano usam cada um. */
export async function configuracaoDaEscola(escolaId) {
  exigirEscola(escolaId);
  const escola = await api.escolas.obter(escolaId);
  if (!escola) throw new NaoEncontrado();
  const cfg = await api.configuracao();
  const dia = hoje();
  const turmas = await api.turmas.listar({ escolaId, ano: cfg.anoAtual });
  const turnos = [];
  for (const turno of turnosDaEscola(escola)) {
    const versoes = await aulasDaSemana(escolaId, turno);
    const atual = versaoNaData(versoes, dia);
    turnos.push({
      turno,
      envio: escola.horarioEnvio?.[turno] || null,
      versoes,
      atual,
      semana: atual?.aulas || null,
      total: totalSemana(atual?.aulas),
      futuras: (versoes || []).filter((v) => v.vigencia > dia),
      anteriores: (versoes || []).filter((v) => v.vigencia <= dia && v !== atual).reverse(),
      turmas: turmas.filter((t) => t.turno === turno).length,
    });
  }
  return { escola, turnos, inicioAno: cfg.anosLetivos[cfg.anoAtual].inicio };
}

// ---------- Alteração ----------

export function validarHorario(valor, campo) {
  const texto = String(valor || '').trim();
  if (texto && !HORA_RE.test(texto)) validar({ [campo]: 'Use o formato HH:MM, por exemplo 09:40.' });
  return texto || null;
}

/**
 * Lê a grade digitada no formulário. Campos: d{dia}_{k}_ini e d{dia}_{k}_fim.
 * Devolve { dias: { 1: [{ ordem, inicio, fim }], ... }, total, erros }.
 * As aulas de cada dia são ordenadas pelo início; a ordem (1ª, 2ª...) sai daí.
 */
export function lerGrade(dados) {
  const erros = {};
  const dias = {};
  let total = 0;
  for (const dia of DIAS_LETIVOS) {
    const linhas = [];
    for (const [nome, valor] of Object.entries(dados)) {
      const m = nome.match(new RegExp(`^d${dia}_(\\d+)_ini$`));
      if (!m) continue;
      const k = m[1];
      const inicio = String(valor || '').trim();
      const fim = String(dados[`d${dia}_${k}_fim`] || '').trim();
      if (!inicio && !fim) continue;
      const nIni = `d${dia}_${k}_ini`;
      const nFim = `d${dia}_${k}_fim`;
      if (!HORA_RE.test(inicio)) { erros[nIni] = 'Informe o início.'; continue; }
      if (!HORA_RE.test(fim)) { erros[nFim] = 'Informe o término.'; continue; }
      if (fim <= inicio) { erros[nFim] = 'O término precisa ser depois do início.'; continue; }
      linhas.push({ inicio, fim, nIni });
    }
    linhas.sort((a, b) => a.inicio.localeCompare(b.inicio));
    for (let i = 1; i < linhas.length; i++) {
      if (linhas[i].inicio < linhas[i - 1].fim) erros[linhas[i].nIni] = 'Esta aula começa antes de a anterior terminar.';
    }
    if (linhas.length > MAX_AULAS_DIA) erros[linhas[0].nIni] = `No máximo ${MAX_AULAS_DIA} aulas por dia.`;
    dias[dia] = linhas.map((l, i) => ({ ordem: i + 1, inicio: l.inicio, fim: l.fim }));
    total += linhas.length;
  }
  return { dias, total, erros };
}

/** Grava uma versão da grade (todas as aulas da semana com a mesma data de vigência). */
async function gravarVersao(escolaId, turno, dias, vigencia) {
  for (const linha of await api.horarios.listar({ escolaId, turno, vigencia })) await api.horarios.remover(linha.id);
  for (const dia of DIAS_LETIVOS) {
    for (const a of dias[dia]) await api.horarios.criar({ escolaId, turno, dia, ordem: a.ordem, inicio: a.inicio, fim: a.fim, vigencia });
  }
}

/**
 * Salva um turno: horário de envio e grade de horários.
 * - Primeira grade do turno: vale desde o início do ano letivo.
 * - Mudança: data de hoje em diante (hoje só se ainda não houver ocorrência
 *   lançada hoje no turno). Mesma data de uma versão existente: substitui.
 * - Turno que já tem grade não volta a ficar sem ela.
 */
export async function salvarTurno(dados) {
  const { escolaId, turno, envio, vigencia } = dados;
  exigirEscola(escolaId);
  const escola = await api.escolas.obter(escolaId);
  if (!escola) throw new NaoEncontrado();
  if (!turnosDaEscola(escola).includes(turno)) validar({ _: 'Este turno não faz parte do modelo de turno da escola.' });
  const cfg = await api.configuracao();
  const dia = hoje();
  const versoes = await aulasDaSemana(escolaId, turno);

  const horario = validarHorario(envio, 'envio');
  const grade = lerGrade(dados);
  const erros = { ...grade.erros };
  if (versoes && !grade.total) erros._ = 'Este turno já tem grade de horários e não pode ficar sem ela, porque a frequência já calculada depende dela.';
  if (versoes && grade.total) {
    erros.vigencia = !vigencia ? 'Informe a partir de quando a grade nova vale.'
      : vigencia < dia ? 'Use hoje ou uma data futura. Os dias que já passaram mantêm a grade que valia.' : '';
  }
  validar(erros);

  const inicio = versoes ? vigencia : cfg.anosLetivos[cfg.anoAtual].inicio;
  const mudouGrade = grade.total && !(versoes && mesmaGrade(versaoNaData(versoes, inicio)?.dias || {}, grade.dias));
  if (versoes && mudouGrade && inicio === dia) {
    const turmasDoTurno = (await api.turmas.listar({ escolaId, turno })).map((t) => t.id);
    const deHoje = await api.lancamentos.listar({ escolaId, data: dia, status: 'ativo', turmaId: turmasDoTurno });
    if (deHoje.length) validar({ vigencia: 'Já há ocorrências lançadas hoje neste turno. Use a partir de amanhã.' });
  }

  await api.escolas.atualizar(escolaId, { horarioEnvio: { ...(escola.horarioEnvio || {}), [turno]: horario } });
  if (!mudouGrade) return { criouVersao: false };
  await gravarVersao(escolaId, turno, grade.dias, inicio);
  return { criouVersao: true, vigencia: inicio, primeira: !versoes };
}

/** Desfaz uma mudança de grade que ainda não começou a valer. */
export async function cancelarMudanca({ escolaId, turno, vigencia }) {
  exigirEscola(escolaId);
  if (!vigencia || vigencia <= hoje()) throw new RegraNegocio('Só é possível cancelar mudanças que ainda não começaram a valer.');
  const linhas = await api.horarios.listar({ escolaId, turno, vigencia });
  if (!linhas.length) throw new NaoEncontrado();
  for (const linha of linhas) await api.horarios.remover(linha.id);
}

/** Texto curto do turno para frases: "turno da manhã", "turno integral". */
export const nomeDoTurno = (turno) => TURNO_EXTENSO[turno] || 'turno';

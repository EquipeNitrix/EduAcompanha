// Frequência: lançamento diário e cálculo do percentual.
//
// Regras:
// - A secretaria registra só quem faltou, chegou atrasado, saiu mais cedo ou saiu e
//   voltou; os demais são considerados presentes.
// - As aulas perdidas saem da grade de horários do turno da turma (tela Turnos e
//   envio, tabela HORARIO_AULA), na versão em vigor na data. Regra da chamada: a
//   aula conta como presente se o aluno estava na escola no início dela.
//   * falta: todas as aulas do dia;
//   * atraso (hora de entrada): as aulas que começaram antes da entrada;
//   * saída antecipada (hora de saída): as aulas que começam a partir da saída;
//   * saiu e voltou (saída e volta): as aulas que começam nesse intervalo.
//   As aulas perdidas não são gravadas no lançamento: são calculadas pela grade.
// - Saída em dois passos: na hora em que o aluno sai, a secretaria registra a saída
//   (saída antecipada). Se ele voltar, ela registra a volta nesse mesmo registro, que
//   passa a ser "saiu e voltou".
// - Uma falta exclui as outras ocorrências do dia. Atraso: no máximo um por dia. Saída
//   sem volta: no máximo uma em aberto. Os horários não podem se sobrepor.
// - Lançamento nunca é apagado: é cancelado com motivo.

import { api } from '../../data/api.js';
import { usuarioAtual, exigirPerfil, exigirEscola } from '../../app/session.js';
import { LIMITE_FREQUENCIA, TIPOS_OCORRENCIA } from '../../app/constantes.js';
import { somarDias, diaSemana, hoje, ehDiaUtil, fmtData, DIAS_SEMANA } from '../../utils/datas.js';
import { agruparPor, indexarPor, porNome } from '../../utils/listas.js';
import { RegraNegocio, NaoEncontrado, validar } from '../../utils/validacao.js';
import { interromperSeSemFalta } from '../notificacao/notificacao.service.js';
import { mapaDeAulas, aulasDaSemana, aulasNaData, aulasDoDia, aulasPerdidas, horarioEnvio, nomeDoTurno, periodoDoDia } from '../configuracao/configuracao.service.js';

const HORA_RE = /^([01]\d|2[0-3]):[0-5]\d$/;

// ---------- Cálculo (funções puras) ----------

/** Aulas perdidas por um lançamento ([{ ordem, inicio, fim }]), pela grade em vigor na data. */
export const perdidasDo = (lancamento, versoes) => aulasPerdidas(versoes, lancamento.data, lancamento) || [];

/**
 * Percentual de presença de uma matrícula:
 * (aulas previstas até hoje - aulas perdidas) / aulas previstas.
 * `versoes` = grade de horários do turno da turma (null = turno não configurado).
 * Numa mesma data, uma aula só é contada uma vez, mesmo com duas ocorrências.
 */
export function calcularFrequencia(matricula, versoes, lancamentos, anoLetivo) {
  const ativos = lancamentos.filter((l) => l.status === 'ativo');
  const diasFalta = ativos.filter((l) => l.tipo === 'falta').length;
  if (!versoes) return { previstas: 0, aulasFalta: 0, diasFalta, pct: null, emAlerta: false, semConfiguracao: true };
  const inicio = matricula.inicio > anoLetivo.inicio ? matricula.inicio : anoLetivo.inicio;
  let fim = hoje();
  if (matricula.fim && matricula.fim < fim) fim = matricula.fim;
  if (anoLetivo.fim < fim) fim = anoLetivo.fim;

  let previstas = 0;
  for (let d = inicio; d <= fim; d = somarDias(d, 1)) previstas += aulasNaData(versoes, d);
  let aulasFalta = 0;
  for (const doDia of agruparPor(ativos.filter((l) => l.data >= inicio && l.data <= fim), 'data').values()) {
    aulasFalta += new Set(doDia.flatMap((l) => perdidasDo(l, versoes).map((a) => a.ordem))).size;
  }
  const pct = previstas ? Math.max(0, ((previstas - aulasFalta) / previstas) * 100) : null;
  return { previstas, aulasFalta, diasFalta, pct, emAlerta: pct != null && pct < LIMITE_FREQUENCIA };
}

/** Acrescenta `perdidas` (lista) e `qtdAulas` (quantidade) a cada lançamento, para as telas. */
export const comAulas = (lancamentos, versoesDaTurma) => lancamentos.map((l) => {
  const versoes = versoesDaTurma(l.turmaId);
  const perdidas = versoes ? perdidasDo(l, versoes) : null;
  return { ...l, perdidas, qtdAulas: perdidas ? perdidas.length : null };
});

/** "chegou 09:30", "saiu 14:00", "saiu 09:30 e voltou 13:00". */
export function horarioDoLancamento(l) {
  if (l.tipo === 'atraso') return `chegou ${l.horaEntrada}`;
  if (l.tipo === 'saida') return `saiu ${l.horaSaida}`;
  if (l.tipo === 'saidaRetorno') return `saiu ${l.horaSaida} e voltou ${l.horaEntrada}`;
  return '';
}

// ---------- Carregamento em lote (para listas e painéis) ----------

/** Carrega turmas, aulas por dia e lançamentos de uma escola (ou de todas) de uma vez. */
export async function carregarBase({ escolaId } = {}) {
  const cfg = await api.configuracao();
  const turmas = await api.turmas.listar({ escolaId });
  const turmaPorId = indexarPor(turmas);
  const aulas = await mapaDeAulas(escolaId);
  const lancamentos = await api.lancamentos.listar({ escolaId });
  const semanaDaTurma = (turmaId) => { const t = turmaPorId.get(turmaId); return t ? aulas.obter(t.escolaId, t.turno) : null; };
  return { cfg, turmas, turmaPorId, semanaDaTurma, lancPorMatricula: agruparPor(lancamentos, 'matriculaId') };
}

export function frequenciaDe(matricula, base) {
  return calcularFrequencia(
    matricula,
    base.semanaDaTurma(matricula.turmaId),
    base.lancPorMatricula.get(matricula.id) || [],
    base.cfg.anosLetivos[matricula.ano],
  );
}

// ---------- Telas de lançamento ----------

export async function turmasParaLancamento() {
  exigirPerfil('secretaria');
  const usuario = usuarioAtual();
  const cfg = await api.configuracao();
  const dia = hoje();
  const turmas = (await api.turmas.listar({ escolaId: usuario.escolaId, ano: cfg.anoAtual })).sort(porNome);
  return Promise.all(turmas.map(async (turma) => {
    const semana = await aulasDaSemana(turma.escolaId, turma.turno);
    const alunos = (await api.matriculas.listar({ turmaId: turma.id, status: 'ativa' })).length;
    const registros = await api.lancamentos.listar({ turmaId: turma.id, data: dia, status: 'ativo' });
    return {
      turma,
      alunos,
      semana,
      aulasHoje: aulasNaData(semana, dia),
      faltasHoje: registros.filter((r) => r.tipo === 'falta').length,
      envio: await horarioEnvio(turma.escolaId, turma.turno),
    };
  }));
}

export async function dadosDoLancamento(turmaId, data) {
  exigirPerfil('secretaria');
  const turma = await api.turmas.obter(turmaId);
  if (!turma) throw new NaoEncontrado();
  exigirEscola(turma.escolaId);
  const cfg = await api.configuracao();
  const anoLetivo = cfg.anosLetivos[turma.ano];
  const semana = await aulasDaSemana(turma.escolaId, turma.turno);
  const matriculas = (await api.matriculas.listar({ turmaId })).filter((m) => m.inicio <= data && (!m.fim || m.fim >= data));
  const alunos = indexarPor(await api.alunos.listar({ id: matriculas.map((m) => m.alunoId) }));
  const lancTurma = await api.lancamentos.listar({ turmaId });
  const porMatricula = agruparPor(lancTurma, 'matriculaId');
  const doDia = lancTurma.filter((l) => l.data === data);
  const linhas = matriculas
    .map((m) => ({
      matricula: m,
      aluno: alunos.get(m.alunoId),
      freq: calcularFrequencia(m, semana, porMatricula.get(m.id) || [], cfg.anosLetivos[m.ano]),
      registros: comAulas(doDia.filter((l) => l.matriculaId === m.id && l.status === 'ativo'), () => semana),
    }))
    .sort((a, b) => porNome(a.aluno, b.aluno));
  return {
    turma,
    anoLetivo,
    anoAtual: cfg.anoAtual,
    semana,
    aulasNoDia: aulasNaData(semana, data),
    aulasDoDia: aulasDoDia(semana, data) || [],
    linhas,
    doDia: comAulas(doDia, () => semana).map((l) => ({ ...l, aluno: alunos.get(l.alunoId) })),
    envio: await horarioEnvio(turma.escolaId, turma.turno),
    notificacoesDoDia: await api.notificacoes.listar({ turmaId, data }),
  };
}

// ---------- Operações ----------

/** Confere as horas de cada tipo e devolve { horaEntrada, horaSaida } normalizados. */
function lerHoras(tipo, dados, aulas) {
  const entrada = String(dados.horaEntrada || '').trim();
  const saida = String(dados.horaSaida || '').trim();
  const p = periodoDoDia(aulas);
  const erros = {};
  const precisaEntrada = tipo === 'atraso' || tipo === 'saidaRetorno';
  const precisaSaida = tipo === 'saida' || tipo === 'saidaRetorno';
  if (precisaEntrada && !HORA_RE.test(entrada)) erros.horaEntrada = tipo === 'atraso' ? 'Informe a hora em que o aluno chegou.' : 'Informe a hora em que o aluno voltou.';
  if (precisaSaida && !HORA_RE.test(saida)) erros.horaSaida = 'Informe a hora em que o aluno saiu.';
  if (!Object.keys(erros).length && p) {
    if (precisaEntrada && (entrada <= p.inicio || entrada >= p.fim)) erros.horaEntrada = `Use um horário entre ${p.inicio} e ${p.fim}, o período de aulas do dia.`;
    if (precisaSaida && (saida <= p.inicio || saida >= p.fim)) erros.horaSaida = `Use um horário entre ${p.inicio} e ${p.fim}, o período de aulas do dia.`;
    if (tipo === 'saidaRetorno' && !erros.horaEntrada && entrada <= saida) erros.horaEntrada = 'A volta precisa ser depois da saída.';
  }
  validar(erros);
  return { horaEntrada: precisaEntrada ? entrada : null, horaSaida: precisaSaida ? saida : null };
}

/** Prévia para a tela: quais aulas a ocorrência faria o aluno perder. Não grava nada. */
export async function previaDePerdidas({ turmaId, data, tipo, horaEntrada, horaSaida }) {
  const turma = await api.turmas.obter(turmaId);
  const versoes = await aulasDaSemana(turma.escolaId, turma.turno);
  if (tipo !== 'falta' && ((tipo !== 'saida' && !HORA_RE.test(horaEntrada || '')) || (tipo !== 'atraso' && !HORA_RE.test(horaSaida || '')))) return null;
  return aulasPerdidas(versoes, data, { tipo, horaEntrada, horaSaida });
}

/** Aulas do dia da turma (para mostrar no formulário da ocorrência). */
export async function aulasDaTurmaNoDia(turmaId, data) {
  const turma = await api.turmas.obter(turmaId);
  return aulasDoDia(await aulasDaSemana(turma.escolaId, turma.turno), data) || [];
}

const sobrepoe = (a, b) => {
  const ini = (l) => (l.tipo === 'atraso' ? '00:00' : l.horaSaida);
  const fim = (l) => (l.tipo === 'saida' ? '23:59' : l.horaEntrada);
  return ini(a) < fim(b) && ini(b) < fim(a);
};

export async function lancar({ matriculaId, data, tipo, horaEntrada, horaSaida, observacao }) {
  exigirPerfil('secretaria');
  if (!TIPOS_OCORRENCIA[tipo]) throw new RegraNegocio('Tipo de registro inválido.');
  const matricula = await api.matriculas.obter(matriculaId);
  if (!matricula) throw new NaoEncontrado();
  const turma = await api.turmas.obter(matricula.turmaId);
  exigirEscola(turma.escolaId);
  const cfg = await api.configuracao();
  const anoLetivo = cfg.anosLetivos[turma.ano];

  if (turma.ano !== cfg.anoAtual) throw new RegraNegocio('Esta turma é de um ano letivo encerrado. Não é possível lançar frequência.');
  if (!data || data > hoje()) throw new RegraNegocio('Escolha uma data até hoje.');
  if (data < anoLetivo.inicio || data > anoLetivo.fim) throw new RegraNegocio(`A data precisa estar dentro do ano letivo (${fmtData(anoLetivo.inicio)} a ${fmtData(anoLetivo.fim)}).`);
  if (!ehDiaUtil(data)) throw new RegraNegocio('Não há aula aos sábados e domingos.');
  if (matricula.inicio > data || (matricula.fim && matricula.fim < data)) throw new RegraNegocio('O aluno não estava matriculado nesta turma nesta data.');

  const versoes = await aulasDaSemana(turma.escolaId, turma.turno);
  const aulas = aulasDoDia(versoes, data);
  if (aulas === null) throw new RegraNegocio(`A escola ainda não montou a grade de horários do ${nomeDoTurno(turma.turno)}. Configure em "Turnos e envio".`);
  if (!aulas.length) throw new RegraNegocio(`O ${nomeDoTurno(turma.turno)} não tem aula na ${DIAS_SEMANA[diaSemana(data)].toLowerCase()}.`);
  const horas = tipo === 'falta' ? { horaEntrada: null, horaSaida: null } : lerHoras(tipo, { horaEntrada, horaSaida }, aulas);
  const texto = String(observacao || '').trim().slice(0, 200) || null;

  const doDia = await api.lancamentos.listar({ matriculaId, data, status: 'ativo' });
  const nome = TIPOS_OCORRENCIA[tipo].toLowerCase();
  if (tipo === 'falta' && doDia.length) throw new RegraNegocio('O aluno tem outra ocorrência neste dia, então esteve na escola. Cancele essa ocorrência antes de lançar falta.');
  if (tipo !== 'falta' && doDia.some((l) => l.tipo === 'falta')) throw new RegraNegocio('O aluno está com falta neste dia. Use "Chegou atrasado" ou cancele a falta antes.');
  if (tipo === 'atraso' && doDia.some((l) => l.tipo === 'atraso')) throw new RegraNegocio(`Este aluno já tem ${nome} registrado neste dia.`);
  if (tipo === 'saida' && doDia.some((l) => l.tipo === 'saida')) throw new RegraNegocio('Este aluno já tem uma saída sem volta neste dia. Registre a volta dela antes de lançar outra saída.');
  const novo = { tipo, ...horas };
  if (doDia.some((l) => sobrepoe(novo, l))) throw new RegraNegocio('Este horário se sobrepõe a outra ocorrência do aluno neste dia.');
  const perdidas = aulasPerdidas(versoes, data, novo);
  if (tipo !== 'falta' && perdidas.length === aulas.length) {
    throw new RegraNegocio(`Com esse horário o aluno perde todas as ${aulas.length} aulas do dia. Nesse caso, registre falta.`);
  }

  const criado = await api.lancamentos.criar({
    alunoId: matricula.alunoId,
    matriculaId,
    turmaId: turma.id,
    escolaId: turma.escolaId,
    data,
    tipo,
    ...horas,
    observacao: texto,
    status: 'ativo',
    criadoPor: usuarioAtual().id,
    criadoEm: new Date().toISOString(),
    motivoCancelamento: null,
    canceladoEm: null,
    canceladoPor: null,
  });
  return { ...criado, perdidas, qtdAulas: perdidas.length };
}

/**
 * O aluno que saiu voltou: completa a saída com a hora da volta.
 * A saída antecipada vira "saiu e voltou" e as aulas depois da volta deixam de contar.
 */
export async function registrarVolta(id, horaEntrada) {
  exigirPerfil('secretaria');
  const saida = await api.lancamentos.obter(id);
  if (!saida) throw new NaoEncontrado();
  exigirEscola(saida.escolaId);
  if (saida.tipo !== 'saida' || saida.status !== 'ativo') throw new RegraNegocio('Só é possível registrar a volta de uma saída sem volta.');
  const turma = await api.turmas.obter(saida.turmaId);
  const versoes = await aulasDaSemana(turma.escolaId, turma.turno);
  const aulas = aulasDoDia(versoes, saida.data) || [];
  const horas = lerHoras('saidaRetorno', { horaEntrada, horaSaida: saida.horaSaida }, aulas);
  const novo = { tipo: 'saidaRetorno', ...horas };
  const outros = (await api.lancamentos.listar({ matriculaId: saida.matriculaId, data: saida.data, status: 'ativo' })).filter((l) => l.id !== id);
  if (outros.some((l) => sobrepoe(novo, l))) validar({ horaEntrada: 'Este horário se sobrepõe a outra ocorrência do aluno neste dia.' });
  const atualizado = await api.lancamentos.atualizar(id, { tipo: 'saidaRetorno', horaEntrada: horas.horaEntrada });
  const perdidas = aulasPerdidas(versoes, saida.data, novo);
  return { ...atualizado, perdidas, qtdAulas: perdidas.length };
}

export async function obterLancamento(id) {
  const lancamento = await api.lancamentos.obter(id);
  if (!lancamento) throw new NaoEncontrado();
  exigirEscola(lancamento.escolaId);
  const turma = await api.turmas.obter(lancamento.turmaId);
  const semana = await aulasDaSemana(turma.escolaId, turma.turno);
  const [aluno, [notificacao]] = await Promise.all([
    api.alunos.obter(lancamento.alunoId),
    api.notificacoes.listar({ alunoId: lancamento.alunoId, data: lancamento.data }),
  ]);
  const [comQtd] = comAulas([lancamento], () => semana);
  return { lancamento: comQtd, aluno, notificacao: notificacao || null };
}

export async function cancelar(id, motivo) {
  exigirPerfil('secretaria');
  const texto = String(motivo || '').trim();
  validar({ motivo: texto.length < 5 ? 'Explique o motivo em pelo menos 5 caracteres.' : '' });
  const lancamento = await api.lancamentos.obter(id);
  if (!lancamento) throw new NaoEncontrado();
  exigirEscola(lancamento.escolaId);
  if (lancamento.status !== 'ativo') throw new RegraNegocio('Este registro já foi cancelado.');
  const atualizado = await api.lancamentos.atualizar(id, {
    status: 'cancelado', motivoCancelamento: texto, canceladoEm: new Date().toISOString(), canceladoPor: usuarioAtual().id,
  });
  if (lancamento.tipo === 'falta') await interromperSeSemFalta(lancamento.alunoId, lancamento.data);
  return atualizado;
}

/**
 * O aluno chegou depois da conferência em sala: a falta de hoje vira atraso,
 * com a hora de chegada. Se for antes do horário de envio do turno, a mensagem nem sai.
 */
export async function chegouAtrasado(id, horaEntrada) {
  exigirPerfil('secretaria');
  const falta = await api.lancamentos.obter(id);
  if (!falta) throw new NaoEncontrado();
  exigirEscola(falta.escolaId);
  if (falta.tipo !== 'falta' || falta.status !== 'ativo') throw new RegraNegocio('Só é possível trocar uma falta registrada.');
  if (falta.data !== hoje()) throw new RegraNegocio('"Chegou atrasado" vale para faltas de hoje. Para outras datas, cancele a falta e registre o atraso.');
  const aulas = await aulasDaTurmaNoDia(falta.turmaId, falta.data);
  const horas = lerHoras('atraso', { horaEntrada }, aulas);
  const turma = await api.turmas.obter(falta.turmaId);
  const perdidas = aulasPerdidas(await aulasDaSemana(turma.escolaId, turma.turno), falta.data, { tipo: 'atraso', ...horas });
  if (perdidas.length === aulas.length) validar({ horaEntrada: 'Com essa hora o aluno perdeu todas as aulas do dia: mantenha a falta.' });
  const agora = new Date().toISOString();
  await api.lancamentos.atualizar(id, { status: 'cancelado', motivoCancelamento: 'Aluno chegou atrasado depois da conferência em sala.', canceladoEm: agora, canceladoPor: usuarioAtual().id });
  const atraso = await api.lancamentos.criar({
    alunoId: falta.alunoId, matriculaId: falta.matriculaId, turmaId: falta.turmaId, escolaId: falta.escolaId, data: falta.data,
    tipo: 'atraso', ...horas, observacao: null, status: 'ativo', criadoPor: usuarioAtual().id, criadoEm: agora,
    motivoCancelamento: null, canceladoEm: null, canceladoPor: null,
  });
  const notificacao = await interromperSeSemFalta(falta.alunoId, falta.data);
  return { atraso, notificacao, perdidas };
}

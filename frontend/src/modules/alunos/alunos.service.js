// Alunos, matrículas, responsáveis e telefones.
//
// - Só o administrador cadastra aluno e mexe em matrícula.
// - A secretaria pode editar os dados do aluno e dos responsáveis da escola dela.
// - Todo aluno tem pelo menos um telefone de responsável (nunca do próprio aluno).
// - Os telefones têm uma ordem de contato: Principal, Secundário, Secundário 2...
//   A mensagem de falta segue essa ordem.
// - Um aluno não pode ter duas matrículas ativas no mesmo ano letivo.

import { api } from '../../data/api.js';
import { exigirPerfil, exigirEscola, escolaDoUsuario } from '../../app/session.js';
import { PARENTESCOS } from '../../app/constantes.js';
import { fmtData } from '../../utils/datas.js';
import { agruparPor, indexarPor, porNome } from '../../utils/listas.js';
import { normalizar, limparEspacos } from '../../utils/texto.js';
import { rotuloTelefone, somenteDigitos } from '../../utils/formatos.js';
import { validar, RegraNegocio, NaoEncontrado, TELEFONE_RE, RA_RE } from '../../utils/validacao.js';
import { carregarBase, frequenciaDe, calcularFrequencia, comAulas } from '../frequencia/frequencia.service.js';
import { mapaDeAulas } from '../configuracao/configuracao.service.js';
import { padroesDeFalta } from '../alerta/alerta.service.js';

/** Escola do aluno = escola da matrícula mais recente. */
async function escolaDoAluno(alunoId) {
  const matriculas = (await api.matriculas.listar({ alunoId })).sort((a, b) => b.ano - a.ano || b.inicio.localeCompare(a.inicio));
  if (!matriculas.length) return null;
  return (await api.turmas.obter(matriculas[0].turmaId))?.escolaId || null;
}

async function exigirAcessoAoAluno(alunoId) {
  const aluno = await api.alunos.obter(alunoId);
  if (!aluno) throw new NaoEncontrado();
  exigirEscola(await escolaDoAluno(alunoId));
  return aluno;
}

// ---------- Consultas ----------

export async function listarAlunos({ q = '', escola = '', turma = '', situacao = '' } = {}) {
  const escolaId = escolaDoUsuario() || escola || undefined;
  const base = await carregarBase({ escolaId });
  const todasTurmas = indexarPor(await api.turmas.listar());
  const matriculas = await api.matriculas.listar({ turmaId: base.turmas.map((t) => t.id) });
  const porAluno = agruparPor(matriculas, 'alunoId');
  const alunos = await api.alunos.listar({ id: [...porAluno.keys()] });
  const comCaso = new Set((await api.casos.listar({ status: 'aberto' })).map((c) => c.alunoId));
  const anoAtual = base.cfg.anoAtual;
  const termo = normalizar(q);
  const digitos = somenteDigitos(q);

  const linhas = [];
  for (const aluno of alunos) {
    const todas = (await api.matriculas.listar({ alunoId: aluno.id })).sort((a, b) => b.ano - a.ano || b.inicio.localeCompare(a.inicio));
    const escolaAtual = todasTurmas.get(todas[0].turmaId)?.escolaId;
    if (escolaId && escolaAtual !== escolaId) continue;
    const ativa = todas.find((m) => m.ano === anoAtual && m.status === 'ativa');
    const turmaAtual = ativa ? todasTurmas.get(ativa.turmaId) : null;
    const freq = ativa ? frequenciaDe(ativa, base) : null;
    const sit = !ativa ? 'sem' : freq.emAlerta ? 'alerta' : 'regular';
    if (termo && !normalizar(aluno.nome).includes(termo) && !(digitos && somenteDigitos(aluno.ra).includes(digitos))) continue;
    if (turma && turmaAtual?.id !== turma) continue;
    if (situacao && sit !== situacao) continue;
    linhas.push({ aluno, turma: turmaAtual, escolaId: escolaAtual, freq, situacao: sit, casoAberto: comCaso.has(aluno.id) });
  }
  return linhas.sort((a, b) => porNome(a.aluno, b.aluno));
}

export async function opcoesDeFiltro({ escola = '' } = {}) {
  const cfg = await api.configuracao();
  const escolaId = escolaDoUsuario() || escola || undefined;
  const escolas = (await api.escolas.listar()).filter((e) => !escolaDoUsuario() || e.id === escolaDoUsuario()).sort(porNome);
  const turmas = (await api.turmas.listar({ ano: cfg.anoAtual, escolaId })).sort(porNome);
  return { escolas, turmas, cfg };
}

export async function contatosDoAluno(alunoId) {
  const responsaveis = (await api.responsaveis.listar({ alunoId })).sort(porNome);
  const porId = indexarPor(responsaveis);
  const telefones = (await api.telefones.listar({ alunoId }))
    .sort((a, b) => a.ordem - b.ordem)
    .map((t, i) => ({ ...t, rotulo: rotuloTelefone(i), responsavel: porId.get(t.responsavelId) }));
  return {
    telefones,
    responsaveis: responsaveis.map((r) => ({ ...r, telefones: telefones.filter((t) => t.responsavelId === r.id) })),
  };
}

export async function perfilDoAluno(id) {
  const aluno = await exigirAcessoAoAluno(id);
  const cfg = await api.configuracao();
  const matriculas = (await api.matriculas.listar({ alunoId: id })).sort((a, b) => b.ano - a.ano || b.inicio.localeCompare(a.inicio));
  const turmas = indexarPor(await api.turmas.listar({ id: matriculas.map((m) => m.turmaId) }));
  const escolas = indexarPor(await api.escolas.listar());
  const lancamentos = await api.lancamentos.listar({ alunoId: id });
  const porMatricula = agruparPor(lancamentos, 'matriculaId');
  const aulas = await mapaDeAulas();
  const semanaDaTurma = (turmaId) => { const t = turmas.get(turmaId); return t ? aulas.obter(t.escolaId, t.turno) : null; };

  const listaMatriculas = matriculas.map((m) => ({
    matricula: m,
    turma: turmas.get(m.turmaId),
    escola: escolas.get(turmas.get(m.turmaId).escolaId),
    freq: calcularFrequencia(m, semanaDaTurma(m.turmaId), porMatricula.get(m.id) || [], cfg.anosLetivos[m.ano]),
  }));
  const ativa = listaMatriculas.find((x) => x.matricula.ano === cfg.anoAtual && x.matricula.status === 'ativa') || null;
  const usuarios = indexarPor(await api.usuarios.listar());

  return {
    aluno,
    cfg,
    escola: escolas.get(turmas.get(matriculas[0].turmaId).escolaId),
    matriculas: listaMatriculas,
    ativa,
    lancamentos: comAulas(lancamentos, semanaDaTurma).map((l) => ({ ...l, autor: usuarios.get(l.criadoPor)?.nome || '-' })),
    contatos: await contatosDoAluno(id),
    notificacoes: (await api.notificacoes.listar({ alunoId: id })).sort((a, b) => b.data.localeCompare(a.data)),
    casos: (await api.casos.listar({ alunoId: id })).sort((a, b) => b.abertura.localeCompare(a.abertura)),
    padroes: await padroesDeFalta(id),
  };
}

// ---------- Cadastro do aluno (administrador) ----------

async function raEmUso(ra, ignorarId) {
  const alvo = somenteDigitos(ra);
  return (await api.alunos.listar()).some((a) => a.id !== ignorarId && somenteDigitos(a.ra) === alvo);
}

function regrasTelefone(numero, obrigatorio) {
  if (!numero) return obrigatorio ? 'Informe o telefone.' : '';
  return TELEFONE_RE.test(numero) ? '' : 'Use o formato (15) 99999-9999.';
}

export async function criarAluno(dados) {
  exigirPerfil('admin');
  const cfg = await api.configuracao();
  const anoLetivo = cfg.anosLetivos[cfg.anoAtual];
  const nome = limparEspacos(dados.nome);
  const ra = String(dados.ra || '').trim();
  const responsavel = limparEspacos(dados.responsavelNome);
  validar({
    ra: !ra ? 'Informe o RA.' : !RA_RE.test(ra) ? 'Use só números, com o dígito no final. Exemplo: 000123456789-0' : '',
    nome: !nome ? 'Informe o nome completo.' : nome.split(' ').length < 2 ? 'Informe nome e sobrenome.' : '',
    turmaId: !dados.turmaId ? 'Escolha a turma.' : '',
    inicio: !dados.inicio ? 'Informe a data de início.' : dados.inicio < anoLetivo.inicio || dados.inicio > anoLetivo.fim ? `Use uma data entre ${fmtData(anoLetivo.inicio)} e ${fmtData(anoLetivo.fim)}.` : '',
    responsavelNome: !responsavel ? 'Informe o nome do responsável.' : '',
    parentesco: !PARENTESCOS.includes(dados.parentesco) ? 'Escolha o parentesco.' : '',
    telefone1: regrasTelefone(dados.telefone1, true),
    telefone2: regrasTelefone(dados.telefone2, false) || (dados.telefone2 && dados.telefone2 === dados.telefone1 ? 'Os telefones precisam ser diferentes.' : ''),
  });
  if (await raEmUso(ra)) validar({ ra: 'Já existe um aluno com este RA.' });
  const turma = await api.turmas.obter(dados.turmaId);
  if (!turma || turma.ano !== cfg.anoAtual) validar({ turmaId: 'Escolha uma turma do ano letivo atual.' });

  const aluno = await api.alunos.criar({ ra, nome });
  await api.matriculas.criar({ alunoId: aluno.id, turmaId: turma.id, ano: turma.ano, inicio: dados.inicio, fim: null, status: 'ativa' });
  const resp = await api.responsaveis.criar({ alunoId: aluno.id, nome: responsavel, parentesco: dados.parentesco });
  await api.telefones.criar({ alunoId: aluno.id, responsavelId: resp.id, numero: dados.telefone1, ordem: 0 });
  if (dados.telefone2) await api.telefones.criar({ alunoId: aluno.id, responsavelId: resp.id, numero: dados.telefone2, ordem: 1 });
  return aluno;
}

/** Secretaria (da escola) e administrador podem editar. */
export async function editarAluno(id, { ra, nome }) {
  await exigirAcessoAoAluno(id);
  const texto = limparEspacos(nome);
  const codigo = String(ra || '').trim();
  validar({
    ra: !RA_RE.test(codigo) ? 'Use só números, com o dígito no final.' : '',
    nome: texto.split(' ').length < 2 ? 'Informe nome e sobrenome.' : '',
  });
  if (await raEmUso(codigo, id)) validar({ ra: 'Já existe um aluno com este RA.' });
  return api.alunos.atualizar(id, { ra: codigo, nome: texto });
}

// ---------- Responsáveis e telefones (secretaria e administrador) ----------

async function renumerar(alunoId) {
  const telefones = (await api.telefones.listar({ alunoId })).sort((a, b) => a.ordem - b.ordem);
  for (let i = 0; i < telefones.length; i++) if (telefones[i].ordem !== i) await api.telefones.atualizar(telefones[i].id, { ordem: i });
}

async function numeroRepetido(alunoId, numero, ignorarId) {
  return (await api.telefones.listar({ alunoId })).some((t) => t.id !== ignorarId && t.numero === numero);
}

export async function adicionarTelefone({ alunoId, responsavelId, novoNome, novoParentesco, numero }) {
  await exigirAcessoAoAluno(alunoId);
  const novo = responsavelId === '__novo';
  validar({
    responsavelId: !responsavelId ? 'Escolha o responsável.' : '',
    novoNome: novo && !limparEspacos(novoNome) ? 'Informe o nome do responsável.' : '',
    novoParentesco: novo && !PARENTESCOS.includes(novoParentesco) ? 'Escolha o parentesco.' : '',
    numero: regrasTelefone(numero, true),
  });
  if (await numeroRepetido(alunoId, numero)) validar({ numero: 'Este número já está na lista do aluno.' });
  let idResponsavel = responsavelId;
  if (novo) idResponsavel = (await api.responsaveis.criar({ alunoId, nome: limparEspacos(novoNome), parentesco: novoParentesco })).id;
  const ordem = (await api.telefones.listar({ alunoId })).length;
  return api.telefones.criar({ alunoId, responsavelId: idResponsavel, numero, ordem });
}

export async function editarTelefone(id, { numero, responsavelId }) {
  const telefone = await api.telefones.obter(id);
  if (!telefone) throw new NaoEncontrado();
  await exigirAcessoAoAluno(telefone.alunoId);
  validar({ numero: regrasTelefone(numero, true), responsavelId: !responsavelId ? 'Escolha o responsável.' : '' });
  if (await numeroRepetido(telefone.alunoId, numero, id)) validar({ numero: 'Este número já está na lista do aluno.' });
  const anterior = telefone.responsavelId;
  const salvo = await api.telefones.atualizar(id, { numero, responsavelId });
  await removerResponsavelSemTelefone(anterior);
  return salvo;
}

async function removerResponsavelSemTelefone(responsavelId) {
  if (!(await api.telefones.listar({ responsavelId })).length) await api.responsaveis.remover(responsavelId);
}

export async function removerTelefone(id) {
  const telefone = await api.telefones.obter(id);
  if (!telefone) throw new NaoEncontrado();
  await exigirAcessoAoAluno(telefone.alunoId);
  if ((await api.telefones.listar({ alunoId: telefone.alunoId })).length <= 1) {
    throw new RegraNegocio('Este é o único telefone do aluno. Adicione outro antes de remover este.');
  }
  await api.telefones.remover(id);
  await removerResponsavelSemTelefone(telefone.responsavelId);
  await renumerar(telefone.alunoId);
}

export async function moverTelefone(id, direcao) {
  const telefone = await api.telefones.obter(id);
  if (!telefone) throw new NaoEncontrado();
  await exigirAcessoAoAluno(telefone.alunoId);
  const lista = (await api.telefones.listar({ alunoId: telefone.alunoId })).sort((a, b) => a.ordem - b.ordem);
  const i = lista.findIndex((t) => t.id === id);
  const j = direcao === 'subir' ? i - 1 : i + 1;
  if (j < 0 || j >= lista.length) return;
  await api.telefones.atualizar(lista[i].id, { ordem: j });
  await api.telefones.atualizar(lista[j].id, { ordem: i });
}

export async function editarResponsavel(id, { nome, parentesco }) {
  const responsavel = await api.responsaveis.obter(id);
  if (!responsavel) throw new NaoEncontrado();
  await exigirAcessoAoAluno(responsavel.alunoId);
  const texto = limparEspacos(nome);
  validar({ nome: !texto ? 'Informe o nome.' : '', parentesco: !PARENTESCOS.includes(parentesco) ? 'Escolha o parentesco.' : '' });
  return api.responsaveis.atualizar(id, { nome: texto, parentesco });
}

export async function removerResponsavel(id) {
  const responsavel = await api.responsaveis.obter(id);
  if (!responsavel) throw new NaoEncontrado();
  await exigirAcessoAoAluno(responsavel.alunoId);
  const doResponsavel = await api.telefones.listar({ responsavelId: id });
  const total = (await api.telefones.listar({ alunoId: responsavel.alunoId })).length;
  if (total - doResponsavel.length < 1) throw new RegraNegocio('O aluno ficaria sem nenhum telefone. Cadastre outro responsável antes de remover este.');
  for (const t of doResponsavel) await api.telefones.remover(t.id);
  await api.responsaveis.remover(id);
  await renumerar(responsavel.alunoId);
}

// ---------- Matrículas (administrador) ----------

export async function alunosSemMatriculaNaTurma(turmaId) {
  exigirPerfil('admin');
  const turma = await api.turmas.obter(turmaId);
  const todasTurmas = indexarPor(await api.turmas.listar());
  const alunos = await api.alunos.listar();
  const resultado = [];
  for (const aluno of alunos) {
    const matriculas = (await api.matriculas.listar({ alunoId: aluno.id })).sort((a, b) => b.ano - a.ano || b.inicio.localeCompare(a.inicio));
    if (!matriculas.length) continue;
    if (todasTurmas.get(matriculas[0].turmaId)?.escolaId !== turma.escolaId) continue;
    if (matriculas.some((m) => m.ano === turma.ano && m.status === 'ativa')) continue;
    resultado.push(aluno);
  }
  return resultado.sort(porNome);
}

export async function matricular({ alunoId, turmaId, inicio }) {
  exigirPerfil('admin');
  validar({ alunoId: !alunoId ? 'Escolha o aluno.' : '', turmaId: !turmaId ? 'Escolha a turma.' : '' });
  const turma = await api.turmas.obter(turmaId);
  const cfg = await api.configuracao();
  const anoLetivo = cfg.anosLetivos[turma.ano];
  if (turma.ano !== cfg.anoAtual) throw new RegraNegocio('Só é possível matricular em turmas do ano letivo atual.');
  validar({ inicio: !inicio ? 'Informe a data.' : inicio < anoLetivo.inicio || inicio > anoLetivo.fim ? `Use uma data entre ${fmtData(anoLetivo.inicio)} e ${fmtData(anoLetivo.fim)}.` : '' });
  const ativas = await api.matriculas.listar({ alunoId, ano: turma.ano, status: 'ativa' });
  if (ativas.length) validar({ alunoId: 'Este aluno já tem matrícula ativa neste ano letivo. Encerre a atual antes.' });
  return api.matriculas.criar({ alunoId, turmaId, ano: turma.ano, inicio, fim: null, status: 'ativa' });
}

export async function encerrarMatricula(id, fim) {
  exigirPerfil('admin');
  const matricula = await api.matriculas.obter(id);
  if (!matricula) throw new NaoEncontrado();
  if (matricula.status !== 'ativa') throw new RegraNegocio('Esta matrícula já está encerrada.');
  validar({ fim: !fim ? 'Informe a data.' : fim < matricula.inicio ? 'A data não pode ser anterior ao início da matrícula.' : '' });
  return api.matriculas.atualizar(id, { status: 'encerrada', fim });
}

// Busca Ativa Escolar: só a secretaria da escola abre, conduz e encerra casos.
// Um aluno pode ter apenas um caso aberto por vez. O sistema nunca abre um caso sozinho.
//
// Tentativas de contato e ações ficam na mesma coleção (tabela ACOMPANHAMENTO_CASO),
// separadas por `tipoRegistro`: 'tentativa' ou 'acao'. Na tela continuam em dois formulários.

import { api } from '../../data/api.js';
import { usuarioAtual, exigirPerfil, exigirEscola, escolaDoUsuario } from '../../app/session.js';
import { hoje, fmtData } from '../../utils/datas.js';
import { indexarPor, porNome } from '../../utils/listas.js';
import { normalizar } from '../../utils/texto.js';
import { validar, RegraNegocio, NaoEncontrado } from '../../utils/validacao.js';
import { carregarBase, frequenciaDe } from '../frequencia/frequencia.service.js';
import { filaDeContatos } from '../notificacao/notificacao.service.js';

export async function contarCasosAbertos() {
  return (await api.casos.listar({ status: 'aberto', escolaId: escolaDoUsuario() })).length;
}

export async function listarCasos({ status = 'aberto', q = '' } = {}) {
  exigirPerfil('secretaria');
  const escolaId = usuarioAtual().escolaId;
  const base = await carregarBase({ escolaId });
  const casos = await api.casos.listar({ escolaId });
  const alunos = indexarPor(await api.alunos.listar({ id: casos.map((c) => c.alunoId) }));
  const matriculas = await api.matriculas.listar({ alunoId: casos.map((c) => c.alunoId), ano: base.cfg.anoAtual, status: 'ativa' });
  const tentativas = await api.acompanhamentos.listar({ casoId: casos.map((c) => c.id), tipoRegistro: 'tentativa' });
  const termo = normalizar(q);
  const linhas = casos.map((caso) => {
    const matricula = matriculas.find((m) => m.alunoId === caso.alunoId);
    const doCaso = tentativas.filter((t) => t.casoId === caso.id).sort((a, b) => b.dataHora.localeCompare(a.dataHora));
    return {
      caso,
      aluno: alunos.get(caso.alunoId),
      turma: matricula ? base.turmaPorId.get(matricula.turmaId) : null,
      freq: matricula ? frequenciaDe(matricula, base) : null,
      tentativas: doCaso.length,
      ultimaTentativa: doCaso[0] || null,
    };
  });
  return {
    contagem: { aberto: casos.filter((c) => c.status === 'aberto').length, encerrado: casos.filter((c) => c.status === 'encerrado').length },
    linhas: linhas
      .filter((l) => l.caso.status === status && (!termo || normalizar(l.aluno.nome).includes(termo)))
      .sort((a, b) => b.caso.abertura.localeCompare(a.caso.abertura)),
  };
}

export async function obterCaso(id) {
  exigirPerfil('secretaria');
  const caso = await api.casos.obter(id);
  if (!caso) throw new NaoEncontrado();
  exigirEscola(caso.escolaId);
  const cfg = await api.configuracao();
  const aluno = await api.alunos.obter(caso.alunoId);
  const [matricula] = await api.matriculas.listar({ alunoId: aluno.id, ano: cfg.anoAtual, status: 'ativa' });
  let turma = null;
  let freq = null;
  if (matricula) {
    const base = await carregarBase({ escolaId: caso.escolaId });
    turma = base.turmaPorId.get(matricula.turmaId);
    freq = frequenciaDe(matricula, base);
  }
  const responsaveis = (await api.responsaveis.listar({ alunoId: aluno.id })).sort(porNome);
  const porId = indexarPor(responsaveis);
  const registros = (await api.acompanhamentos.listar({ casoId: id })).sort((a, b) => a.dataHora.localeCompare(b.dataHora));
  const tentativas = registros.filter((r) => r.tipoRegistro === 'tentativa').map((r) => ({ ...r, responsavel: porId.get(r.responsavelId) || null }));
  const acoes = registros.filter((r) => r.tipoRegistro === 'acao');
  return { caso, aluno, turma, freq, contatos: await filaDeContatos(aluno.id), responsaveis, tentativas, acoes };
}

/** Alunos da escola com matrícula ativa e sem caso aberto, da menor para a maior frequência. */
export async function candidatosACaso() {
  exigirPerfil('secretaria');
  const escolaId = usuarioAtual().escolaId;
  const base = await carregarBase({ escolaId });
  const matriculas = (await api.matriculas.listar({ ano: base.cfg.anoAtual, status: 'ativa' })).filter((m) => base.turmaPorId.has(m.turmaId));
  const comCaso = new Set((await api.casos.listar({ escolaId, status: 'aberto' })).map((c) => c.alunoId));
  const alunos = indexarPor(await api.alunos.listar({ id: matriculas.map((m) => m.alunoId) }));
  return matriculas
    .filter((m) => !comCaso.has(m.alunoId))
    .map((m) => ({ aluno: alunos.get(m.alunoId), freq: frequenciaDe(m, base) }))
    .sort((a, b) => (a.freq.pct ?? 100) - (b.freq.pct ?? 100) || porNome(a.aluno, b.aluno));
}

export async function casoAbertoDoAluno(alunoId) {
  const [caso] = await api.casos.listar({ alunoId, status: 'aberto' });
  return caso || null;
}

export async function abrirCaso({ alunoId, abertura, motivo }) {
  exigirPerfil('secretaria');
  validar({
    alunoId: !alunoId ? 'Escolha o aluno.' : '',
    abertura: !abertura ? 'Informe a data de abertura.' : abertura > hoje() ? 'A data não pode ser futura.' : '',
  });
  const escolaId = usuarioAtual().escolaId;
  const cfg = await api.configuracao();
  const [matricula] = await api.matriculas.listar({ alunoId, ano: cfg.anoAtual, status: 'ativa' });
  const turma = matricula ? await api.turmas.obter(matricula.turmaId) : null;
  if (!turma || turma.escolaId !== escolaId) throw new RegraNegocio('O aluno precisa ter matrícula ativa nesta escola.');
  if (await casoAbertoDoAluno(alunoId)) validar({ alunoId: 'Este aluno já tem um caso aberto.' });
  return api.casos.criar({
    alunoId, escolaId, abertura, motivo: String(motivo || '').trim(), status: 'aberto',
    encerramento: null, desfecho: null, observacaoFinal: '', criadoPor: usuarioAtual().id,
  });
}

async function casoAbertoParaEditar(casoId) {
  exigirPerfil('secretaria');
  const caso = await api.casos.obter(casoId);
  if (!caso) throw new NaoEncontrado();
  exigirEscola(caso.escolaId);
  if (caso.status !== 'aberto') throw new RegraNegocio('Este caso está encerrado e aceita apenas consulta.');
  return caso;
}

function validarDataHora(dataHora, caso, oQue) {
  if (!dataHora) return 'Informe data e hora.';
  if (dataHora.slice(0, 10) < caso.abertura) return `${oQue} não pode ser anterior à abertura do caso (${fmtData(caso.abertura)}).`;
  if (new Date(dataHora) > new Date()) return 'A data e hora não podem ser futuras.';
  return '';
}

export async function registrarTentativa({ casoId, canal, dataHora, descricao, resultado, responsavelId }) {
  const caso = await casoAbertoParaEditar(casoId);
  validar({
    canal: !canal ? 'Escolha o canal.' : '',
    dataHora: validarDataHora(dataHora, caso, 'A tentativa'),
    descricao: String(descricao || '').trim().length < 5 ? 'Descreva a conversa em pelo menos 5 caracteres.' : '',
    resultado: !resultado ? 'Escolha o resultado.' : '',
  });
  if (responsavelId) {
    const responsavel = await api.responsaveis.obter(responsavelId);
    if (!responsavel || responsavel.alunoId !== caso.alunoId) validar({ responsavelId: 'Escolha um responsável deste aluno.' });
  }
  return api.acompanhamentos.criar({
    casoId, tipoRegistro: 'tentativa', dataHora, descricao: descricao.trim(),
    canal, resultado, tipoAcao: null, responsavelId: responsavelId || null, registradoPor: usuarioAtual().id,
  });
}

export async function registrarAcao({ casoId, tipoAcao, dataHora, descricao }) {
  const caso = await casoAbertoParaEditar(casoId);
  validar({
    tipoAcao: !tipoAcao ? 'Escolha o tipo.' : '',
    dataHora: validarDataHora(dataHora, caso, 'A ação'),
    descricao: String(descricao || '').trim().length < 5 ? 'Descreva a ação em pelo menos 5 caracteres.' : '',
  });
  return api.acompanhamentos.criar({
    casoId, tipoRegistro: 'acao', dataHora, descricao: descricao.trim(),
    canal: null, resultado: null, tipoAcao, responsavelId: null, registradoPor: usuarioAtual().id,
  });
}

export async function encerrarCaso({ id, data, desfecho, observacao }) {
  const caso = await casoAbertoParaEditar(id);
  validar({
    data: !data ? 'Informe a data.' : data < caso.abertura ? 'A data não pode ser anterior à abertura.' : data > hoje() ? 'A data não pode ser futura.' : '',
    desfecho: !desfecho ? 'Escolha o desfecho.' : '',
  });
  return api.casos.atualizar(id, { status: 'encerrado', encerramento: data, desfecho, observacaoFinal: String(observacao || '').trim() });
}

// Notificação automática aos responsáveis.
//
// processarFila() é a rotina que, no sistema real, roda no SERVIDOR a cada minuto.
// Na demonstração ela roda no navegador (main.js chama a cada 30 segundos).

import { api } from '../../data/api.js';
import { usuarioAtual, escolaDoUsuario, exigirEscola, exigirPerfil } from '../../app/session.js';
import { hoje, ehDiaUtil } from '../../utils/datas.js';
import { indexarPor, unicos } from '../../utils/listas.js';
import { normalizar } from '../../utils/texto.js';
import { rotuloTelefone } from '../../utils/formatos.js';
import { validar, RegraNegocio, NaoEncontrado } from '../../utils/validacao.js';
import { criarNotificacao, avancar, PRAZO_MS } from './fila.js';
import { horarioEnvio } from '../configuracao/configuracao.service.js';
import { turnosDaEscola } from '../../app/constantes.js';

/** Telefones do aluno na ordem de contato, já com nome do responsável e rótulo. */
export async function filaDeContatos(alunoId) {
  const telefones = (await api.telefones.listar({ alunoId })).sort((a, b) => a.ordem - b.ordem);
  const responsaveis = indexarPor(await api.responsaveis.listar({ alunoId }));
  return telefones.map((t, i) => {
    const r = responsaveis.get(t.responsavelId) || { nome: 'Responsável', parentesco: '' };
    return { telefoneId: t.id, numero: t.numero, nome: r.nome, parentesco: r.parentesco, rotulo: rotuloTelefone(i) };
  });
}

/**
 * 1. Avança a fila das notificações aguardando resposta (próximo telefone após 24 h).
 * 2. A partir do horário de envio do turno, manda a mensagem de quem tem falta no dia (uma por aluno por dia).
 *    Turno sem horário de envio não manda mensagens.
 */
export async function processarFila() {
  const agora = Date.now();
  const dia = hoje();
  let enviadas = 0;
  let mudancas = 0;

  for (const notificacao of await api.notificacoes.listar({ status: 'aguardando' })) {
    if (avancar(notificacao, agora)) {
      await api.notificacoes.atualizar(notificacao.id, notificacao);
      mudancas++;
    }
  }

  if (!ehDiaUtil(dia)) return { enviadas, mudancas };

  const faltas = await api.lancamentos.listar({ data: dia, tipo: 'falta', status: 'ativo' });
  if (!faltas.length) return { enviadas, mudancas };
  const jaNotificados = new Set((await api.notificacoes.listar({ data: dia })).map((n) => n.alunoId));
  const turmas = indexarPor(await api.turmas.listar({ id: unicos(faltas.map((f) => f.turmaId)) }));
  const escolas = indexarPor(await api.escolas.listar());
  const horarios = {};

  for (const alunoId of unicos(faltas.map((f) => f.alunoId))) {
    if (jaNotificados.has(alunoId)) continue;
    const falta = faltas.find((f) => f.alunoId === alunoId);
    const turma = turmas.get(falta.turmaId);
    const chave = `${turma.escolaId}:${turma.turno}`;
    if (!(chave in horarios)) horarios[chave] = await horarioEnvio(turma.escolaId, turma.turno);
    const envio = horarios[chave];
    if (!envio) continue;
    const disparo = new Date(`${dia}T${envio}:00`).getTime();
    if (agora < disparo) continue;
    const aluno = await api.alunos.obter(alunoId);
    const notificacao = criarNotificacao({ aluno, turma, escola: escolas.get(turma.escolaId), data: dia, fila: await filaDeContatos(alunoId), enviarEm: agora });
    await api.notificacoes.criar(notificacao);
    enviadas++;
  }
  return { enviadas, mudancas };
}

/** Quando a falta do dia é cancelada, as próximas tentativas de contato param. */
export async function interromperSeSemFalta(alunoId, data) {
  const [notificacao] = await api.notificacoes.listar({ alunoId, data });
  if (!notificacao) return null;
  const faltasAtivas = await api.lancamentos.listar({ alunoId, data, tipo: 'falta', status: 'ativo' });
  if (faltasAtivas.length || notificacao.status !== 'aguardando') return notificacao;
  return api.notificacoes.atualizar(notificacao.id, {
    status: 'interrompida',
    encerradaEm: new Date().toISOString(),
    observacao: 'A falta foi cancelada pela secretaria. As próximas tentativas não serão enviadas.',
  });
}

export async function contarAguardando() {
  const escolaId = escolaDoUsuario();
  return (await api.notificacoes.listar({ status: 'aguardando', escolaId })).length;
}

export async function listarNotificacoes({ escola = '', status = '', q = '' } = {}) {
  const escolaId = escolaDoUsuario() || escola || undefined;
  const lista = await api.notificacoes.listar({ escolaId, status: status || undefined });
  const alunos = indexarPor(await api.alunos.listar({ id: unicos(lista.map((n) => n.alunoId)) }));
  const turmas = indexarPor(await api.turmas.listar({ id: unicos(lista.map((n) => n.turmaId)) }));
  const termo = normalizar(q);
  return lista
    .map((n) => ({ notificacao: n, aluno: alunos.get(n.alunoId), turma: turmas.get(n.turmaId) }))
    .filter((x) => !termo || normalizar(x.aluno?.nome).includes(termo))
    .sort((a, b) => b.notificacao.data.localeCompare(a.notificacao.data) || a.aluno.nome.localeCompare(b.aluno.nome, 'pt-BR'));
}

export async function detalheNotificacao(id) {
  const notificacao = await api.notificacoes.obter(id);
  if (!notificacao) throw new NaoEncontrado();
  exigirEscola(notificacao.escolaId);
  const [aluno, turma] = await Promise.all([api.alunos.obter(notificacao.alunoId), api.turmas.obter(notificacao.turmaId)]);
  const faltaAtiva = (await api.lancamentos.listar({ alunoId: notificacao.alunoId, data: notificacao.data, tipo: 'falta', status: 'ativo' })).length > 0;
  return { notificacao, aluno, turma, faltaAtiva };
}

/** Na demonstração, simula a resposta que chegaria pela API do WhatsApp. */
export async function registrarResposta(id, texto) {
  validar({ resposta: !String(texto || '').trim() ? 'Escreva a resposta recebida.' : '' });
  const notificacao = await api.notificacoes.obter(id);
  if (!notificacao) throw new NaoEncontrado();
  exigirEscola(notificacao.escolaId);
  if (notificacao.status !== 'aguardando') throw new RegraNegocio('Esta notificação já foi encerrada.');
  return api.notificacoes.atualizar(id, { status: 'respondida', resposta: texto.trim(), respondidoEm: new Date().toISOString() });
}

/** Simulação: faz as notificações aguardando "envelhecerem" 24 h e roda a fila. */
export async function simularPassagemDe24h() {
  const escolaId = escolaDoUsuario();
  const aguardando = await api.notificacoes.listar({ status: 'aguardando', escolaId });
  for (const n of aguardando) {
    const tentativas = n.tentativas.map((t) => ({ ...t, enviadoEm: new Date(Date.parse(t.enviadoEm) - PRAZO_MS).toISOString() }));
    await api.notificacoes.atualizar(n.id, { tentativas });
  }
  const resultado = await processarFila();
  return { afetadas: aguardando.length, ...resultado };
}

/** Situação do envio automático de hoje, por escola e turno. */
export async function statusDisparos(escolaIds) {
  const cfg = await api.configuracao();
  const dia = hoje();
  const agora = Date.now();
  const notificacoesHoje = await api.notificacoes.listar({ data: dia });
  const saida = [];
  for (const escolaId of escolaIds) {
    const turmas = await api.turmas.listar({ escolaId, ano: cfg.anoAtual });
    for (const turno of turnosDaEscola(await api.escolas.obter(escolaId))) {
      const doTurno = turmas.filter((t) => t.turno === turno);
      if (!doTurno.length) continue;
      const ids = doTurno.map((t) => t.id);
      const envio = await horarioEnvio(escolaId, turno);
      const enviadas = notificacoesHoje.filter((n) => ids.includes(n.turmaId)).length;
      let situacao;
      if (!ehDiaUtil(dia)) situacao = { texto: 'Sem aula hoje', tom: 'muted' };
      else if (!envio) situacao = { texto: 'Sem horário de envio', tom: 'crit' };
      else if (agora >= new Date(`${dia}T${envio}:00`).getTime()) {
        situacao = { texto: enviadas ? `Enviado · ${enviadas} ${enviadas > 1 ? 'mensagens' : 'mensagem'}` : 'Concluído · nenhuma falta', tom: 'ok' };
      } else situacao = { texto: `Agendado para ${envio}`, tom: 'info' };
      saida.push({ escolaId, turno, envio, situacao });
    }
  }
  return saida;
}

export function exigirSecretaria() { exigirPerfil('secretaria'); return usuarioAtual(); }

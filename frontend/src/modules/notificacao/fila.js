// Regra da fila de contato, sem acesso a dados (funções puras).
//
// 1. No horário de envio do turno, a mensagem vai para o telefone Principal.
// 2. Sem resposta em 24 horas, vai para o próximo da lista (Secundário, Secundário 2...).
// 3. Se o envio falhar (número inválido), passa na hora para o próximo.
// 4. Acabou a lista sem resposta: a notificação é encerrada "sem resposta".
// 5. Uma resposta a qualquer momento interrompe a fila.

import { PRAZO_RESPOSTA_HORAS } from '../../app/constantes.js';
import { fmtData } from '../../utils/datas.js';
import { primeiroNome } from '../../utils/texto.js';

export const PRAZO_MS = PRAZO_RESPOSTA_HORAS * 3600 * 1000;

export function textoMensagem({ escola, aluno, turma, data }) {
  return `Olá! Aqui é a secretaria da ${escola.nome}. ${primeiroNome(aluno.nome)} (${turma.nome}) faltou hoje, ${fmtData(data)}. Pode nos contar o motivo da ausência? É só responder esta mensagem.`;
}

/**
 * Simulação do envio pela API do WhatsApp.
 * Números terminados em 0000 falham, para demonstrar que uma falha não trava a fila.
 */
export const simularEntrega = (numero) => !/0000$/.test(numero);

function enviar(notificacao, indice, instanteMs) {
  const contato = notificacao.fila[indice];
  notificacao.tentativas.push({
    indice,
    rotulo: contato.rotulo,
    nome: contato.nome,
    numero: contato.numero,
    enviadoEm: new Date(instanteMs).toISOString(),
    entregue: simularEntrega(contato.numero),
  });
}

/** Monta a notificação e faz o primeiro envio. `fila` = contatos já em ordem. */
export function criarNotificacao({ aluno, turma, escola, data, fila, enviarEm }) {
  const notificacao = {
    alunoId: aluno.id,
    turmaId: turma.id,
    escolaId: turma.escolaId,
    data,
    mensagem: textoMensagem({ escola, aluno, turma, data }),
    fila,
    tentativas: [],
    status: 'aguardando',
    resposta: null,
    respondidoEm: null,
    encerradaEm: null,
    observacao: '',
  };
  if (!fila.length) {
    notificacao.status = 'sem_resposta';
    notificacao.encerradaEm = new Date(enviarEm).toISOString();
    notificacao.observacao = 'O aluno não tem telefone de responsável cadastrado.';
    return notificacao;
  }
  enviar(notificacao, 0, enviarEm);
  avancar(notificacao, enviarEm);
  return notificacao;
}

/**
 * Avança a fila até o instante informado. Altera o objeto recebido.
 * Retorna true se algo mudou (novo envio ou encerramento).
 */
export function avancar(notificacao, agoraMs) {
  let mudou = false;
  while (notificacao.status === 'aguardando') {
    const ultima = notificacao.tentativas[notificacao.tentativas.length - 1];
    const enviadoMs = Date.parse(ultima.enviadoEm);
    const proximoMs = ultima.entregue ? enviadoMs + PRAZO_MS : enviadoMs;
    if (agoraMs < proximoMs) break;

    const proximo = ultima.indice + 1;
    if (proximo < notificacao.fila.length) {
      enviar(notificacao, proximo, proximoMs);
    } else {
      notificacao.status = 'sem_resposta';
      notificacao.encerradaEm = new Date(proximoMs).toISOString();
      notificacao.observacao = ultima.entregue
        ? `Sem resposta em ${PRAZO_RESPOSTA_HORAS} h no último telefone da lista.`
        : 'O envio falhou no último telefone da lista.';
    }
    mudou = true;
  }
  return mudou;
}

// Tela de notificações aos responsáveis.

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { usuarioAtual, ehAdmin, escolaDoUsuario } from '../../app/session.js';
import { STATUS_NOTIFICACAO, TURNOS, PRAZO_RESPOSTA_HORAS } from '../../app/constantes.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { filtroBusca, filtroSelect } from '../../components/filters.js';
import { tabela, botaoMostrarMais } from '../../components/table.js';
import { abrirModal, fecharModal, modalAberto } from '../../components/modal.js';
import { confirmar } from '../../components/confirm.js';
import { campo, botoesModal, botaoCarregando } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc, semPrefixoEscola, plural } from '../../utils/texto.js';
import { fmtData, fmtDataHora, diaSemana, DIAS_SEMANA } from '../../utils/datas.js';
import { mascararTelefone } from '../../utils/formatos.js';
import { porNome } from '../../utils/listas.js';
import {
  processarFila, listarNotificacoes, detalheNotificacao, registrarResposta, simularPassagemDe24h, statusDisparos,
} from './notificacao.service.js';

/** Roda a rotina de envio e avisa na tela quando algo sai. Usado no main.js e após lançamentos. */
export async function rodarRotina({ avisar = true } = {}) {
  try {
    const { enviadas, mudancas } = await processarFila();
    if (!usuarioAtual()) return;
    if (avisar && enviadas) toast(`${enviadas} ${enviadas > 1 ? 'mensagens de falta enviadas' : 'mensagem de falta enviada'} aos responsáveis (simulação).`, 'info');
    if ((enviadas || mudancas) && !modalAberto()) atualizar();
  } catch (erro) {
    console.error('Rotina de notificação', erro);
  }
}

const statusChip = (status) => chip(...STATUS_NOTIFICACAO[status]);

/** Cartão "Aviso automático de hoje", também usado no início. */
export async function cartaoDisparos(escolaIds, mostrarEscola) {
  const situacoes = await statusDisparos(escolaIds);
  const escolas = new Map((await api.escolas.listar()).map((e) => [e.id, e]));
  return `<section class="card">
    <div class="card-h"><h2>Aviso automático de hoje</h2><a href="#/notificacoes" class="small">Ver notificações</a></div>
    ${situacoes.length ? situacoes.map((s) => `<div class="status-line">
      <div>
        <div class="t">${TURNOS[s.turno]}${mostrarEscola ? ` · ${esc(semPrefixoEscola(escolas.get(s.escolaId)?.nome))}` : ''}</div>
        <div class="small muted">${s.envio ? `Envio às ${s.envio}` : 'Sem horário de envio'}</div>
      </div>${chip(s.situacao.texto, s.situacao.tom)}
    </div>`).join('') : '<div class="card-b muted">Nenhuma turma cadastrada no ano letivo.</div>'}
  </section>`;
}

export function tabelaNotificacoes(linhas, { comAluno = true } = {}) {
  return tabela(
    ['Dia da falta', ...(comAluno ? ['Aluno'] : []), 'Último envio', 'Situação', 'Resposta', ''],
    linhas.map(({ notificacao: n, aluno, turma }) => {
      const ultima = n.tentativas[n.tentativas.length - 1];
      return `<tr>
        <td class="num"><b>${fmtData(n.data)}</b><span class="sub">${DIAS_SEMANA[diaSemana(n.data)]}</span></td>
        ${comAluno ? `<td><a class="row-link" href="#/alunos/${aluno.id}">${esc(aluno.nome)}</a><span class="sub">${esc(turma?.nome || '')}</span></td>` : ''}
        <td class="small">${ultima ? `${esc(ultima.rotulo)} · ${fmtDataHora(ultima.enviadoEm)}<span class="sub">${esc(mascararTelefone(ultima.numero))}${ultima.entregue ? '' : ' · envio falhou'}</span>` : '<span class="muted">Não enviada</span>'}</td>
        <td>${statusChip(n.status)}</td>
        <td class="small" style="max-width:280px">${n.resposta ? esc(n.resposta) : n.observacao ? `<span class="muted">${esc(n.observacao)}</span>` : '<span class="muted">-</span>'}</td>
        <td><div class="row-actions"><button class="btn btn-ghost btn-sm" type="button" data-acao="notificacao:detalhes" data-id="${n.id}">Detalhes</button></div></td>
      </tr>`;
    }),
  );
}

async function telaNotificacoes() {
  const f = estado('notificacoes', { escola: '', status: '', q: '' });
  const linhas = await listarNotificacoes(f);
  const limite = Number(estado('mais').notificacoes) || 50;
  const escolas = (await api.escolas.listar()).sort(porNome);
  const idsEscolas = escolaDoUsuario() ? [escolaDoUsuario()] : f.escola ? [f.escola] : escolas.map((e) => e.id);

  return `${cabecalhoPagina({
    titulo: 'Notificações aos responsáveis',
    subtitulo: `Em cada dia com falta, o responsável recebe uma mensagem pelo WhatsApp automática no telefone Principal. `,
  })}
  <div class="grid-3-1">
    ${await cartaoDisparos(idsEscolas, ehAdmin())}
    <section class="card">
      <div class="card-h"><h2>Simulação</h2>${chip('Só na demonstração', 'sun', { semPonto: true })}</div>
      <div class="card-b" style="display:grid;gap:12px">
        <p class="small muted">No sistema real o servidor faz isso sozinho e as respostas chegam pela API do WhatsApp. Estes botões servem para testar o fluxo.</p>
        <div style="display:flex;flex-wrap:wrap;gap:8px">
          <button class="btn btn-ghost btn-sm" type="button" data-acao="notificacao:verificar">${icone('refresh', 15)}Verificar agora</button>
          <button class="btn btn-ghost btn-sm" type="button" data-acao="notificacao:avancar">${icone('fast', 15)}Avançar ${PRAZO_RESPOSTA_HORAS} h</button>
        </div>
        <p class="small muted">Números terminados em 0000 simulam falha de envio: a mensagem passa na hora para o próximo telefone e as outras mensagens seguem normalmente.</p>
      </div>
    </section>
  </div>
  <section class="card">
    <div class="filters">
      ${filtroBusca({ rotulo: 'Aluno', chave: 'notificacoes.q', valor: f.q, placeholder: 'Buscar pelo nome' })}
      ${ehAdmin() ? filtroSelect({ rotulo: 'Escola', chave: 'notificacoes.escola', valor: f.escola, opcoes: escolas.map((e) => [e.id, e.nome]) }) : ''}
      ${filtroSelect({ rotulo: 'Situação', chave: 'notificacoes.status', valor: f.status, opcoes: Object.entries(STATUS_NOTIFICACAO).map(([k, v]) => [k, v[0]]), todas: 'Todas' })}
    </div>
    ${linhas.length
    ? tabelaNotificacoes(linhas.slice(0, limite)) + botaoMostrarMais('notificacoes', linhas.length, limite)
    : estadoVazio({
      icone: 'msg',
      titulo: 'Nenhuma notificação encontrada',
      texto: f.status || f.q ? 'Ajuste a busca ou os filtros.' : 'As mensagens aparecem aqui quando houver falta registrada e chegar o horário de envio do turno.',
      acoes: f.status || f.q ? '<button class="btn btn-ghost btn-sm" type="button" data-acao="filtros:limpar" data-chave="notificacoes">Limpar filtros</button>' : '',
    })}
  </section>`;
}

async function abrirDetalhes(id) {
  const { notificacao: n, aluno, turma, faltaAtiva } = await detalheNotificacao(id);
  const enviados = new Map(n.tentativas.map((t) => [t.indice, t]));
  const fila = n.fila.map((contato, i) => {
    const t = enviados.get(i);
    const situacao = t ? (t.entregue ? chip(`Enviada ${fmtDataHora(t.enviadoEm)}`, 'ok') : chip('Envio falhou', 'crit')) : chip('Não usado', 'muted', { semPonto: true });
    return `<div class="list-item"><span class="contato-ordem ${i === 0 ? 'principal' : ''}">${i + 1}</span>
      <div class="grow"><div class="t">${esc(contato.rotulo)} · ${esc(contato.nome)}</div><div class="s">${esc(contato.parentesco)} · <span class="num">${esc(mascararTelefone(contato.numero))}</span></div></div>${situacao}</div>`;
  }).join('');
  const fim = n.status === 'respondida'
    ? `<div class="tl-item ok"><span class="tl-dot">${icone('ok')}</span><div><div class="when">${fmtDataHora(n.respondidoEm)}</div><div class="what">Resposta recebida</div><div class="body"><div class="msg-bubble">${esc(n.resposta)}</div></div></div></div>`
    : n.status === 'aguardando' ? ''
      : `<div class="tl-item ${n.status === 'sem_resposta' ? 'crit' : 'muted'}"><span class="tl-dot">${icone('x')}</span><div><div class="when">${fmtDataHora(n.encerradaEm)}</div><div class="what">${STATUS_NOTIFICACAO[n.status][0]}</div><div class="body">${esc(n.observacao)}</div></div></div>`;

  abrirModal('Notificação de falta', `
    <dl class="dl">
      <dt>Aluno</dt><dd><a href="#/alunos/${aluno.id}">${esc(aluno.nome)}</a> · ${esc(turma.nome)}</dd>
      <dt>Dia da falta</dt><dd>${fmtData(n.data)}</dd>
      <dt>Situação</dt><dd>${statusChip(n.status)}</dd>
    </dl>
    ${!faltaAtiva ? `<div class="note sun">${icone('info')}<span>A falta deste dia foi cancelada depois do envio.</span></div>` : ''}
    <div><div class="eyebrow" style="margin-bottom:6px">Mensagem enviada</div><div class="msg-bubble">${esc(n.mensagem)}</div></div>
    <div><div class="eyebrow" style="margin-bottom:6px">Ordem de contato</div><div class="card flat"><div class="list">${fila}</div></div></div>
    ${fim ? `<div class="timeline flush">${fim}</div>` : ''}
    <div class="modal-actions">
      ${n.status === 'aguardando' ? `<button class="btn btn-ghost" type="button" data-acao="notificacao:responder" data-id="${n.id}">${icone('msg')}Simular resposta do responsável</button>` : ''}
      <button class="btn btn-primary" type="button" data-acao="modal:fechar">Fechar</button>
    </div>`, { largo: true });
}

export const rotas = [
  { caminho: 'notificacoes', perfis: ['admin', 'secretaria'], tela: telaNotificacoes },
];

registrarAcoes({
  'notificacao:detalhes': (d) => abrirDetalhes(d.id),
  'notificacao:responder': (d) => abrirModal('Simular resposta do responsável', `
    <form data-form="notificacao:resposta" novalidate class="form-grid one">
      <input type="hidden" name="id" value="${esc(d.id)}">
      <div class="note sun">${icone('info')}<span>No sistema real, a resposta chega pela API do WhatsApp. Uma resposta interrompe os próximos envios.</span></div>
      ${campo({ rotulo: 'Motivo informado pelo responsável', nome: 'resposta', tipo: 'textarea', obrigatorio: true, valor: 'Estava com febre, levei ao posto de saúde.', atributos: 'maxlength="500"' })}
      ${botoesModal('Registrar resposta', { largo: false })}
    </form>`),
  'notificacao:verificar': async (_, botao) => {
    botaoCarregando(botao, true);
    const { enviadas, mudancas } = await processarFila();
    atualizar();
    toast(enviadas || mudancas ? `Verificação concluída: ${plural(enviadas, 'mensagem nova', 'mensagens novas')} e ${plural(mudancas, 'mudança de etapa', 'mudanças de etapa')}.` : 'Verificação concluída. Nada pendente agora.', 'info');
  },
  'notificacao:avancar': () => confirmar({
    titulo: `Avançar ${PRAZO_RESPOSTA_HORAS} horas?`,
    texto: `<p>Simula a passagem de ${PRAZO_RESPOSTA_HORAS} horas para as notificações aguardando resposta: a mensagem vai para o próximo telefone da lista ou, se era o último, a notificação é encerrada sem resposta.</p>`,
    botao: `Avançar ${PRAZO_RESPOSTA_HORAS} h`,
    tom: 'primary',
    aoConfirmar: async () => {
      const r = await simularPassagemDe24h();
      atualizar();
      toast(r.afetadas ? `${plural(r.mudancas, 'notificação mudou', 'notificações mudaram')} de etapa.` : 'Nenhuma notificação aguardando resposta.', 'info');
    },
  }),
});

registrarFormularios({
  'notificacao:resposta': async (dados) => {
    await registrarResposta(dados.id, dados.resposta);
    fecharModal();
    atualizar();
    toast('Resposta registrada. Os próximos envios foram interrompidos.');
  },
});

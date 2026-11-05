// Telas de lançamento de frequência (somente secretaria).

import { registrarAcoes, registrarFormularios, tratarErro } from '../../app/eventos.js';
import { atualizar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { TIPOS_OCORRENCIA, TOM_OCORRENCIA, TURNOS } from '../../app/constantes.js';
import { nomeDoTurno } from '../configuracao/configuracao.service.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { confirmar } from '../../components/confirm.js';
import { campo, botoesModal, botaoCarregando } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc, primeiroNome, plural } from '../../utils/texto.js';
import { hoje, horaAgora, fmtData, fmtHora, diaSemana, DIAS_SEMANA, ehDiaUtil, ultimoDiaUtil } from '../../utils/datas.js';
import { fmtPct } from '../../utils/formatos.js';
import { turmasParaLancamento, dadosDoLancamento, lancar, cancelar, chegouAtrasado, registrarVolta, obterLancamento, horarioDoLancamento, aulasDaTurmaNoDia } from './frequencia.service.js';
import { rodarRotina } from '../notificacao/notificacao.page.js';

async function telaTurmas() {
  const cartoes = await turmasParaLancamento();
  const dia = hoje();
  return `${cabecalhoPagina({
    titulo: 'Lançar frequência',
    subtitulo: `Registre apenas quem faltou, chegou atrasado ou saiu mais cedo; os demais alunos são considerados presentes. `,
  })}
  ${cartoes.length ? `<div class="turma-cards">${cartoes.map((c) => `
    <a class="turma-card" href="#/frequencia/${c.turma.id}">
      <div class="chips">${chip(TURNOS[c.turma.turno], 'info', { semPonto: true })}</div>
      <h3>${esc(c.turma.nome)}</h3>
      <div class="row"><span>${plural(c.alunos, 'aluno', 'alunos')}</span><span>${c.aulasHoje === null ? 'Aulas não configuradas' : c.aulasHoje ? `${plural(c.aulasHoje, 'aula', 'aulas')} hoje` : 'Sem aula hoje'}</span></div>
      <div class="row"><span>Faltas registradas hoje</span><b>${c.faltasHoje}</b></div>
      <div class="row"><span>Mensagens aos responsáveis</span><span>${c.envio ? `às ${c.envio}` : 'sem horário'}</span></div>
    </a>`).join('')}</div>`
    : `<section class="card">${estadoVazio({ icone: 'layers', titulo: 'Nenhuma turma no ano letivo', texto: 'Peça ao administrador para cadastrar as turmas da escola.' })}</section>`}`;
}

function avisoDoEnvio(d, data) {
  if (data !== hoje()) return '';
  if (!d.envio) return `<div class="banner sun">${icone('msg')}<span>Este turno não tem horário de envio definido, então os responsáveis não recebem a mensagem de falta. Defina em <a href="#/turnos">Turnos e envio</a>.</span></div>`;
  const enviado = horaAgora() >= d.envio;
  return enviado
    ? `<div class="banner info">${icone('msg')}<span>As mensagens deste turno saíram às ${d.envio}. Se um aluno com falta chegou depois da conferência, use <b>Chegou atrasado</b>: as próximas tentativas de contato são interrompidas.</span></div>`
    : `<div class="banner sun">${icone('clock')}<span>As mensagens de falta deste turno saem às <b>${d.envio}</b>. Até lá, se um aluno com falta chegar, use <b>Chegou atrasado</b> e a mensagem não é enviada.</span></div>`;
}

function acoesDoAluno(linha, turmaId, data) {
  const { registros, matricula, aluno } = linha;
  const falta = registros.find((r) => r.tipo === 'falta');
  const nome = esc(primeiroNome(aluno.nome));
  if (falta) {
    return `${chip(`Falta · ${plural(falta.qtdAulas, 'aula', 'aulas')}`, 'crit')}
      ${data === hoje() ? `<button class="btn btn-ghost btn-sm" type="button" data-acao="frequencia:atrasado" data-id="${falta.id}">${icone('clockBack', 15)}Chegou atrasado</button>` : ''}
      <button class="btn btn-ghost btn-sm" type="button" data-acao="frequencia:cancelar" data-id="${falta.id}">Cancelar</button>`;
  }
  const existentes = registros.map((r) => `<span class="reg">${chip(`${TIPOS_OCORRENCIA[r.tipo]} · ${r.tipo === 'saidaRetorno' ? `${r.horaSaida} às ${r.horaEntrada}` : (r.horaEntrada || r.horaSaida)}${r.qtdAulas != null ? ` · ${plural(r.qtdAulas, 'aula', 'aulas')}` : ''}`, TOM_OCORRENCIA[r.tipo])}
    ${r.tipo === 'saida' ? `<button class="btn btn-ghost btn-sm" type="button" data-acao="frequencia:volta" data-id="${r.id}">${icone('clockBack', 15)}Voltou</button>` : ''}
    <button class="icon-btn" type="button" data-acao="frequencia:cancelar" data-id="${r.id}" aria-label="Cancelar ${TIPOS_OCORRENCIA[r.tipo].toLowerCase()} de ${nome}" title="Cancelar">${icone('x', 15)}</button></span>`).join('');
  const tem = (tipo) => registros.some((r) => r.tipo === tipo);
  const botoes = [
    !registros.length ? `<button type="button" class="falta" data-acao="frequencia:lancar" data-tipo="falta" data-matricula="${matricula.id}" data-turma="${turmaId}" data-nome="${nome}">Falta</button>` : '',
    !tem('atraso') ? `<button type="button" data-acao="frequencia:ocorrencia" data-tipo="atraso" data-matricula="${matricula.id}" data-turma="${turmaId}" data-nome="${nome}">Atraso</button>` : '',
    !tem('saida') ? `<button type="button" data-acao="frequencia:ocorrencia" data-tipo="saida" data-matricula="${matricula.id}" data-turma="${turmaId}" data-nome="${nome}">Saída</button>` : '',
  ].filter(Boolean).join('');
  return `${existentes}${botoes ? `<div class="seg" role="group" aria-label="Registrar para ${esc(aluno.nome)}">${botoes}</div>` : ''}`;
}

async function telaLancamento({ id }) {
  const st = estado(`freq:${id}`, { data: hoje(), q: '' });
  const d = await dadosDoLancamento(id, st.data);
  const data = st.data;
  const encerrada = d.turma.ano !== d.anoAtual;
  const foraDoAno = data < d.anoLetivo.inicio || data > d.anoLetivo.fim;
  const termo = st.q.trim().toLowerCase();
  const linhas = d.linhas.filter((l) => !termo || l.aluno.nome.toLowerCase().includes(termo) || l.aluno.ra.includes(termo));

  let corpo;
  if (encerrada) corpo = estadoVazio({ icone: 'lock', titulo: 'Ano letivo encerrado', texto: 'Turmas de anos anteriores ficam disponíveis só para consulta.' });
  else if (foraDoAno) corpo = estadoVazio({ icone: 'calendar', titulo: 'Data fora do ano letivo', texto: `O ano letivo ${d.turma.ano} vai de ${fmtData(d.anoLetivo.inicio)} a ${fmtData(d.anoLetivo.fim)}.` });
  else if (!ehDiaUtil(data)) {
    const util = ultimoDiaUtil(data);
    corpo = estadoVazio({ icone: 'calendar', titulo: 'Não há aula no fim de semana', texto: 'Escolha um dia de segunda a sexta.', acoes: `<button class="btn btn-primary btn-sm" type="button" data-acao="frequencia:data" data-turma="${id}" data-valor="${util}">Ir para ${fmtData(util)}</button>` });
  } else if (d.aulasNoDia === null) corpo = estadoVazio({ icone: 'clock', titulo: 'Grade de horários não configurada', texto: `Para lançar ocorrências, a escola precisa montar a grade de horários do ${nomeDoTurno(d.turma.turno)}.`, acoes: '<a class="btn btn-primary btn-sm" href="#/turnos">Configurar turno</a>' });
  else if (!d.aulasNoDia) corpo = estadoVazio({ icone: 'calendar', titulo: `Sem aula na ${DIAS_SEMANA[diaSemana(data)].toLowerCase()}`, texto: `O ${nomeDoTurno(d.turma.turno)} não tem aulas neste dia da semana.` });
  else if (!linhas.length) corpo = estadoVazio({ icone: 'users', titulo: st.q ? 'Nenhum aluno encontrado' : 'Nenhum aluno matriculado', texto: st.q ? 'Confira o nome ou o RA digitado.' : 'Peça ao administrador para matricular os alunos nesta turma.' });
  else {
    corpo = linhas.map((l) => `<div class="stu">
      <div style="min-width:0">
        <div class="name"><a href="#/alunos/${l.aluno.id}" style="color:inherit;text-decoration:none">${esc(l.aluno.nome)}</a></div>
        <div class="meta"><span class="mono">RA ${esc(l.aluno.ra)}</span><span>Frequência ${fmtPct(l.freq.pct)}</span>${l.freq.emAlerta ? chip('Em alerta', 'crit') : ''}</div>
      </div>
      <div class="acoes">${acoesDoAluno(l, id, data)}</div>
    </div>`).join('');
  }

  const doDia = d.doDia.slice().sort((a, b) => (a.aluno?.nome || '').localeCompare(b.aluno?.nome || '', 'pt-BR'));
  const ativos = doDia.filter((l) => l.status === 'ativo');
  const ehHoje = data === hoje();

  return `${cabecalhoPagina({
    trilha: [['Lançar frequência', 'frequencia'], [d.turma.nome]],
    titulo: `${esc(d.turma.nome)} <span class="turno">${TURNOS[d.turma.turno]}</span>`,
    subtitulo: encerrada ? '' : '',
  })}
  <div class="grid-3-1">
    <section class="card">
      <div class="filters">
        <div class="filter" style="max-width:190px"><label for="freq_data">Data da chamada</label>
          <input id="freq_data" type="date" value="${data}" max="${hoje()}" min="${d.anoLetivo.inicio}" data-ui="freq:${id}.data"></div>
        <div class="filter grow"><label for="freq_busca">Buscar aluno</label>
          <div class="search-input">${icone('search')}<input id="freq_busca" type="search" placeholder="Nome ou RA" value="${esc(st.q)}" data-ui="freq:${id}.q"></div></div>
      </div>
      ${!encerrada && !foraDoAno && d.aulasNoDia ? `<div class="card-b" style="border-bottom:1px solid var(--line)">
        <div class="eyebrow">${ehHoje ? 'Hoje' : DIAS_SEMANA[diaSemana(data)]}, ${fmtData(data)} · ${plural(d.aulasNoDia, 'aula', 'aulas')} das ${d.aulasDoDia[0].inicio} às ${d.aulasDoDia[d.aulasDoDia.length - 1].fim} · uma falta conta todas</div>
      </div>` : ''}
      ${!encerrada ? avisoDoEnvio(d, data) : ''}
      <div>${corpo}</div>
    </section>

    <section class="card">
      <div class="card-h"><h2>Registros em ${fmtData(data)}</h2>${chip(String(ativos.length), '', { semPonto: true })}</div>
      ${doDia.length ? `<div class="list">${doDia.map((l) => `<div class="list-item">
        <div class="grow">
          <div class="t ${l.status === 'cancelado' ? 'muted' : ''}">${esc(l.aluno?.nome || 'Aluno')}</div>
          <div class="s">${l.status === 'cancelado' ? `Cancelado: ${esc(l.motivoCancelamento)}`
    : `${l.tipo === 'falta' ? 'Dia inteiro' : horarioDoLancamento(l)}${l.qtdAulas != null ? ` · ${plural(l.qtdAulas, 'aula perdida', 'aulas perdidas')}` : ''}${l.observacao ? ` · ${esc(l.observacao)}` : ''}`}</div>
        </div>
        ${l.status === 'cancelado' ? chip('Cancelado', 'muted', { semPonto: true }) : chip(TIPOS_OCORRENCIA[l.tipo], TOM_OCORRENCIA[l.tipo])}
      </div>`).join('')}</div>`
    : '<div class="card-b muted small">Nenhum registro nesta data. Alunos sem registro são considerados presentes.</div>'}
    </section>
  </div>`;
}

export const rotas = [
  { caminho: 'frequencia', perfis: ['secretaria'], tela: telaTurmas },
  { caminho: 'frequencia/:id', perfis: ['secretaria'], tela: telaLancamento, menu: 'frequencia' },
];

async function desfazer(lancamento) {
  try {
    await cancelar(lancamento.id, 'Desfeito pela secretaria logo após o registro.');
    atualizar();
    toast('Registro desfeito. Ele fica no histórico como cancelado.');
  } catch (erro) { tratarErro(erro); }
}

// ---------- Prévia das aulas perdidas no formulário ----------

function blocoPrevia(aulas) {
  return `<div class="ocorrencia-previa">
    <div class="eyebrow" style="margin-bottom:6px">Aulas do dia</div>
    <div class="aulas-dia" id="previa-aulas">${aulas.map((a) => `<span class="aula-pill" data-inicio="${a.inicio}"><b>${a.ordem}ª</b>${a.inicio}</span>`).join('')}</div>
    <p class="small" id="previa-texto" style="margin-top:8px">Informe o horário para ver as aulas perdidas.</p>
  </div>`;
}

/** Mesma regra do service (a aula conta como presente se o aluno estava no início dela). */
function perdidasNaTela(aulas, tipo, entrada, saida) {
  const ok = (h) => /^\d{2}:\d{2}$/.test(h || '');
  if ((tipo !== 'saida' && !ok(entrada)) || (tipo !== 'atraso' && !ok(saida))) return null;
  if (tipo === 'atraso') return aulas.filter((a) => a.inicio < entrada);
  if (tipo === 'saida') return aulas.filter((a) => a.inicio >= saida);
  return aulas.filter((a) => a.inicio >= saida && a.inicio < entrada);
}

function ligarPrevia(aulas) {
  const form = document.querySelector('form[data-previa]');
  if (!form) return;
  const atualizarPrevia = () => {
    const perdidas = perdidasNaTela(aulas, form.dataset.previa, form.horaEntrada?.value, form.horaSaida?.value);
    const ordens = new Set((perdidas || []).map((a) => a.ordem));
    form.querySelectorAll('#previa-aulas .aula-pill').forEach((p, i) => p.classList.toggle('perdida', ordens.has(aulas[i].ordem)));
    const texto = form.querySelector('#previa-texto');
    if (perdidas === null) texto.textContent = 'Informe o horário para ver as aulas perdidas.';
    else if (!perdidas.length) texto.innerHTML = '<b>Não perde nenhuma aula.</b>';
    else if (perdidas.length === aulas.length) texto.innerHTML = '<b>Perde todas as aulas do dia.</b> Nesse caso, registre falta.';
    else texto.innerHTML = `<b>Perde ${plural(perdidas.length, 'aula', 'aulas')}:</b> ${perdidas.map((a) => `${a.ordem}ª`).join(', ')}.`;
  };
  form.addEventListener('input', atualizarPrevia);
  atualizarPrevia();
}

registrarAcoes({
  'frequencia:data': (d) => { estado(`freq:${d.turma}`).data = d.valor; atualizar(); },

  'frequencia:lancar': async (d, botao) => {
    botaoCarregando(botao, true);
    const data = estado(`freq:${d.turma}`).data;
    const lancamento = await lancar({ matriculaId: d.matricula, data, tipo: d.tipo });
    atualizar();
    const extra = d.tipo === 'falta' ? ` (${plural(lancamento.qtdAulas, 'aula', 'aulas')})` : '';
    toast(`${TIPOS_OCORRENCIA[d.tipo]} registrada para ${d.nome}${extra}.`, 'ok', { rotulo: 'Desfazer', executar: () => desfazer(lancamento) });
    if (d.tipo === 'falta') rodarRotina({ avisar: true });
  },

  'frequencia:cancelar': async (d) => {
    const { lancamento: l, aluno, notificacao } = await obterLancamento(d.id);
    abrirModal('Cancelar registro', `
      <form data-form="frequencia:cancelar" novalidate class="form-grid one">
        <input type="hidden" name="id" value="${l.id}">
        <dl class="dl">
          <dt>Aluno</dt><dd>${esc(aluno.nome)}</dd>
          <dt>Data</dt><dd>${fmtData(l.data)}</dd>
          <dt>Tipo</dt><dd>${TIPOS_OCORRENCIA[l.tipo]}</dd>
          ${l.qtdAulas ? `<dt>Aulas</dt><dd>${plural(l.qtdAulas, 'aula', 'aulas')}</dd>` : ''}
        </dl>
        <div class="note">${icone('info')}<span>O registro não é apagado: fica no histórico como cancelado, com o motivo, e deixa de contar na frequência.</span></div>
        ${l.tipo === 'falta' && notificacao ? `<div class="note sun">${icone('msg')}<span>A mensagem de falta deste dia já foi enviada ao responsável. O cancelamento não desfaz o envio, mas interrompe as próximas tentativas.</span></div>` : ''}
        ${campo({ rotulo: 'Motivo do cancelamento', nome: 'motivo', tipo: 'textarea', obrigatorio: true, atributos: 'maxlength="300" placeholder="Exemplo: aluno estava presente, lançado por engano"' })}
        ${botoesModal('Cancelar registro', { tom: 'danger', largo: false })}
      </form>`);
  },

  'frequencia:atrasado': async (d) => {
    const { lancamento: l, aluno, notificacao } = await obterLancamento(d.id);
    const aulas = await aulasDaTurmaNoDia(l.turmaId, l.data);
    const jaEnviada = notificacao && notificacao.tentativas.length;
    abrirModal('Chegou atrasado', `<form data-form="frequencia:atrasado" novalidate class="form-grid one" data-previa="atraso">
      <input type="hidden" name="id" value="${l.id}">
      <p>A falta de hoje de <b>${esc(aluno.nome)}</b> vira atraso. Contam como perdidas só as aulas que começaram antes da chegada.</p>
      ${campo({ prefixo: 'oc', rotulo: 'Hora em que chegou', nome: 'horaEntrada', tipo: 'time', obrigatorio: true, valor: horaAgora() })}
      ${blocoPrevia(aulas)}
      ${jaEnviada ? `<div class="note sun">${icone('msg')}<span>A mensagem de falta já foi enviada às ${fmtHora(notificacao.tentativas[0].enviadoEm)}. As próximas tentativas de contato serão interrompidas.</span></div>`
    : `<div class="note ok">${icone('ok')}<span>A mensagem de falta ainda não saiu e não será enviada.</span></div>`}
      ${botoesModal('Trocar para atraso', { largo: false })}
    </form>`);
    ligarPrevia(aulas);
  },

  'frequencia:volta': async (d) => {
    const { lancamento: l, aluno } = await obterLancamento(d.id);
    const aulas = await aulasDaTurmaNoDia(l.turmaId, l.data);
    abrirModal(`Voltou: ${esc(aluno.nome)}`, `<form data-form="frequencia:volta" novalidate class="form-grid one" data-previa="saidaRetorno">
      <input type="hidden" name="id" value="${l.id}">
      <input type="hidden" name="horaSaida" value="${l.horaSaida}">
      <p class="small muted">${fmtData(l.data)}. Saiu às <b>${l.horaSaida}</b>${l.observacao ? ` (${esc(l.observacao)})` : ''}. Informe a hora em que voltou: as aulas que começam depois disso deixam de contar como perdidas.</p>
      ${campo({ prefixo: 'oc', rotulo: 'Hora em que voltou', nome: 'horaEntrada', tipo: 'time', obrigatorio: true, valor: l.data === hoje() ? horaAgora() : '' })}
      ${blocoPrevia(aulas)}
      ${botoesModal('Registrar volta', { largo: false })}
    </form>`);
    ligarPrevia(aulas);
  },

  'frequencia:ocorrencia': async (d) => {
    const data = estado(`freq:${d.turma}`).data;
    const aulas = await aulasDaTurmaNoDia(d.turma, data);
    const campos = {
      atraso: campo({ prefixo: 'oc', rotulo: 'Hora em que chegou', nome: 'horaEntrada', tipo: 'time', obrigatorio: true }),
      saida: campo({ prefixo: 'oc', rotulo: 'Hora em que saiu', nome: 'horaSaida', tipo: 'time', obrigatorio: true }),
      saidaRetorno: `<div class="form-grid" style="gap:12px">${campo({ prefixo: 'oc', rotulo: 'Hora em que saiu', nome: 'horaSaida', tipo: 'time', obrigatorio: true })}${campo({ prefixo: 'oc', rotulo: 'Hora em que voltou', nome: 'horaEntrada', tipo: 'time', obrigatorio: true })}</div>`,
    };
    const explica = {
      atraso: 'Contam como perdidas as aulas que começaram antes da chegada.',
      saida: 'Registre agora só a hora da saída. Se o aluno voltar, use "Voltou" ao lado do registro: as aulas depois da volta deixam de contar como perdidas.',
      saidaRetorno: 'Para quando o aluno sai e volta no mesmo dia, por exemplo para uma consulta. Contam como perdidas as aulas que começam enquanto ele está fora.',
    };
    abrirModal(`${TIPOS_OCORRENCIA[d.tipo]}: ${esc(d.nome)}`, `<form data-form="frequencia:ocorrencia" novalidate class="form-grid one" data-previa="${d.tipo}">
      <input type="hidden" name="matriculaId" value="${esc(d.matricula)}">
      <input type="hidden" name="turmaId" value="${esc(d.turma)}">
      <input type="hidden" name="tipo" value="${esc(d.tipo)}">
      <input type="hidden" name="nome" value="${esc(d.nome)}">
      <p class="small muted">${fmtData(data)}. ${explica[d.tipo]}</p>
      ${campos[d.tipo]}
      ${campo({ prefixo: 'oc', rotulo: 'Motivo (opcional)', nome: 'observacao', atributos: 'maxlength="200" placeholder="Exemplo: consulta médica com a mãe"' })}
      ${blocoPrevia(aulas)}
      ${botoesModal('Registrar', { largo: false })}
    </form>`);
    ligarPrevia(aulas);
  },
});

registrarFormularios({
  'frequencia:ocorrencia': async (dados) => {
    const data = estado(`freq:${dados.turmaId}`).data;
    const l = await lancar({ ...dados, data });
    fecharModal();
    atualizar();
    toast(`${TIPOS_OCORRENCIA[l.tipo]} registrada para ${dados.nome}: ${plural(l.qtdAulas, 'aula perdida', 'aulas perdidas')}.`, 'ok', { rotulo: 'Desfazer', executar: () => desfazer(l) });
  },
  'frequencia:volta': async (dados) => {
    const r = await registrarVolta(dados.id, dados.horaEntrada);
    fecharModal();
    atualizar();
    toast(`Volta registrada às ${r.horaEntrada}: ${plural(r.qtdAulas, 'aula perdida', 'aulas perdidas')} no total.`);
  },
  'frequencia:atrasado': async (dados) => {
    const r = await chegouAtrasado(dados.id, dados.horaEntrada);
    fecharModal();
    atualizar();
    toast(`Falta trocada por atraso: ${plural(r.perdidas.length, 'aula perdida', 'aulas perdidas')}.`);
  },
  'frequencia:cancelar': async (dados) => {
    await cancelar(dados.id, dados.motivo);
    fecharModal();
    atualizar();
    toast('Registro cancelado. Ele continua no histórico com o motivo.');
  },
});

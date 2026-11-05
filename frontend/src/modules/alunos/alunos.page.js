// Telas de alunos: lista, perfil (frequência, contatos, matrículas, notificações, busca ativa).

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar, navegar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { ehAdmin, ehSecretaria } from '../../app/session.js';
import { LIMITE_FREQUENCIA, TIPOS_OCORRENCIA, TOM_OCORRENCIA, TURNOS, PARENTESCOS, PRAZO_RESPOSTA_HORAS } from '../../app/constantes.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { horarioDoLancamento } from '../frequencia/frequencia.service.js';
import { medidorFrequencia, tomFrequencia } from '../../components/frequency-meter.js';
import { filtroBusca, filtroSelect, filtroData } from '../../components/filters.js';
import { abas } from '../../components/tabs.js';
import { tabela, botaoMostrarMais } from '../../components/table.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { confirmar } from '../../components/confirm.js';
import { campo, botoesModal, ATRIBUTOS_TELEFONE } from '../../components/form.js';
import { abrirExportacao } from '../../components/export.js';
import { toast } from '../../components/toast.js';
import { esc, iniciais, plural } from '../../utils/texto.js';
import { hoje, fmtData, fmtDataHora, diaSemana, DIAS_SEMANA } from '../../utils/datas.js';
import { fmtPct } from '../../utils/formatos.js';
import { porNome } from '../../utils/listas.js';
import {
  listarAlunos, opcoesDeFiltro, perfilDoAluno, criarAluno, editarAluno, contatosDoAluno, adicionarTelefone, editarTelefone,
  removerTelefone, moverTelefone, editarResponsavel, removerResponsavel, matricular, encerrarMatricula,
} from './alunos.service.js';
import { tabelaNotificacoes } from '../notificacao/notificacao.page.js';

const SITUACOES = [['regular', 'Frequência regular'], ['alerta', `Em alerta (abaixo de ${LIMITE_FREQUENCIA}%)`], ['sem', 'Sem matrícula ativa']];

function chipSituacao(situacao) {
  if (situacao === 'alerta') return chip('Em alerta', 'crit');
  if (situacao === 'regular') return chip('Regular', 'ok');
  return chip('Sem matrícula ativa', 'muted', { semPonto: true });
}

// ---------- Lista ----------

async function telaAlunos() {
  const f = estado('alunos', { q: '', escola: '', turma: '', situacao: '' });
  const [linhas, opcoes] = await Promise.all([listarAlunos(f), opcoesDeFiltro(f)]);
  const escolas = new Map(opcoes.escolas.map((e) => [e.id, e]));
  const temFiltro = f.q || f.turma || f.situacao || f.escola;

  return `${cabecalhoPagina({
    titulo: 'Alunos',
    subtitulo: 'Dados do aluno, matrícula por ano letivo, responsáveis e histórico de frequência.',
    acoes: ehAdmin() ? `<button class="btn btn-primary" type="button" data-acao="aluno:novo">${icone('plus')}Novo aluno</button>` : '',
  })}
  <section class="card">
    <div class="filters">
      ${filtroBusca({ chave: 'alunos.q', valor: f.q, placeholder: 'Nome ou RA' })}
      ${ehAdmin() ? filtroSelect({ rotulo: 'Escola', chave: 'alunos.escola', valor: f.escola, opcoes: opcoes.escolas.map((e) => [e.id, e.nome]), limpa: 'alunos.turma' }) : ''}
      ${filtroSelect({ rotulo: `Turma (${opcoes.cfg.anoAtual})`, chave: 'alunos.turma', valor: f.turma, opcoes: opcoes.turmas.map((t) => [t.id, `${t.nome} · ${TURNOS[t.turno]}`]) })}
      ${filtroSelect({ rotulo: 'Situação', chave: 'alunos.situacao', valor: f.situacao, opcoes: SITUACOES })}
    </div>
    ${linhas.length ? `${tabela(
    ['Aluno', `Turma ${opcoes.cfg.anoAtual}`, ...(ehAdmin() ? ['Escola'] : []), 'Frequência', 'Situação'],
    linhas.map((l) => `<tr>
        <td><a class="row-link" href="#/alunos/${l.aluno.id}">${esc(l.aluno.nome)}</a><span class="sub mono">RA ${esc(l.aluno.ra)}</span></td>
        <td>${l.turma ? `${esc(l.turma.nome)}<span class="sub">${TURNOS[l.turma.turno]}</span>` : '<span class="muted">-</span>'}</td>
        ${ehAdmin() ? `<td class="small">${esc(escolas.get(l.escolaId)?.nome || '')}</td>` : ''}
        <td>${l.freq ? medidorFrequencia(l.freq) : '<span class="muted">-</span>'}</td>
        <td><div class="chips">${chipSituacao(l.situacao)}${l.casoAberto ? chip('Busca Ativa', 'sun') : ''}</div></td>
      </tr>`),
  )}<div class="tbl-foot">${plural(linhas.length, 'aluno', 'alunos')}</div>`
    : temFiltro
      ? estadoVazio({ icone: 'search', titulo: 'Nenhum aluno encontrado', texto: 'Ajuste a busca ou os filtros.', acoes: '<button class="btn btn-ghost btn-sm" type="button" data-acao="filtros:limpar" data-chave="alunos">Limpar filtros</button>' })
      : estadoVazio({ icone: 'users', titulo: 'Nenhum aluno cadastrado', texto: ehAdmin() ? 'Cadastre o aluno com RA, nome, turma e o telefone do responsável.' : 'O administrador ainda não cadastrou alunos nesta escola.', acoes: ehAdmin() ? '<button class="btn btn-primary btn-sm" type="button" data-acao="aluno:novo">Novo aluno</button>' : '' })}
  </section>`;
}

// ---------- Perfil ----------

function cartaoFrequencia(p) {
  const f = p.ativa?.freq;
  if (!f) return '<p class="muted">Sem matrícula ativa no ano letivo.</p>';
  if (f.semConfiguracao) return `<div class="note sun">${icone('clock')}<span>A escola ainda não informou quantas aulas o turno da turma tem por dia, então a frequência não pode ser calculada. <a href="#/turnos">Configurar turnos</a>.</span></div>`;
  if (f.pct == null) return '<p class="muted">Ainda não há aulas previstas no período da matrícula.</p>';
  const tom = tomFrequencia(f.pct);
  return `<div style="display:flex;align-items:flex-end;gap:14px;flex-wrap:wrap">
      <span class="big-pct ${tom === 'crit' ? 'crit' : ''}">${fmtPct(f.pct)}</span>
      <span class="muted small" style="padding-bottom:6px">mínimo de ${LIMITE_FREQUENCIA}% exigido pela LDB</span>
    </div>
    <div class="meter ${tom}" style="margin-top:12px;height:10px" role="img" aria-label="Frequência ${fmtPct(f.pct)}, marca em ${LIMITE_FREQUENCIA}%"><i style="width:${Math.min(100, f.pct)}%"></i><span class="mark"></span></div>
    <div class="stat-row">
      <div><b>${f.previstas}</b><span>aulas previstas até hoje</span></div>
      <div><b>${f.aulasFalta}</b><span>aulas com falta</span></div>
      <div><b>${f.diasFalta}</b><span>dias com falta</span></div>
    </div>`;
}

function filtrarHistorico(p, f) {
  const matriculasDoAno = new Set(p.matriculas.filter((m) => String(m.matricula.ano) === String(f.ano)).map((m) => m.matricula.id));
  return p.lancamentos
    .filter((l) => matriculasDoAno.has(l.matriculaId) && (!f.de || l.data >= f.de) && (!f.ate || l.data <= f.ate) && (!f.tipo || l.tipo === f.tipo) && (!f.situacao || l.status === f.situacao))
    .sort((a, b) => b.data.localeCompare(a.data) || a.tipo.localeCompare(b.tipo));
}

function abaHistorico(p) {
  const f = estado(`hist:${p.aluno.id}`, { ano: String(p.cfg.anoAtual), de: '', ate: '', tipo: '', situacao: '' });
  const anos = [...new Set(p.matriculas.map((m) => String(m.matricula.ano)))];
  if (!anos.includes(f.ano)) f.ano = anos[0];
  const lista = filtrarHistorico(p, f);
  const chave = `hist_${p.aluno.id}`;
  const limite = Number(estado('mais')[chave]) || 60;
  return `<div class="filters">
      ${filtroSelect({ rotulo: 'Ano letivo', chave: `hist:${p.aluno.id}.ano`, valor: f.ano, opcoes: anos.map((a) => [a, a]), todas: null })}
      ${filtroData({ rotulo: 'De', chave: `hist:${p.aluno.id}.de`, valor: f.de })}
      ${filtroData({ rotulo: 'Até', chave: `hist:${p.aluno.id}.ate`, valor: f.ate })}
      ${filtroSelect({ rotulo: 'Tipo', chave: `hist:${p.aluno.id}.tipo`, valor: f.tipo, opcoes: Object.entries(TIPOS_OCORRENCIA), todas: 'Todos' })}
      ${filtroSelect({ rotulo: 'Situação', chave: `hist:${p.aluno.id}.situacao`, valor: f.situacao, opcoes: [['ativo', 'Válidos'], ['cancelado', 'Cancelados']], todas: 'Todos' })}
      <button class="btn btn-ghost" type="button" data-acao="aluno:exportarHistorico" data-id="${p.aluno.id}" ${lista.length ? '' : 'disabled'}>${icone('download')}Exportar</button>
    </div>
    ${lista.length ? tabela(
    ['Data', 'Tipo', 'Aulas', 'Situação', 'Registro', ''],
    lista.slice(0, limite).map((l) => `<tr class="${l.status === 'cancelado' ? 'is-cancel' : ''}">
        <td class="num"><b>${fmtData(l.data)}</b><span class="sub">${DIAS_SEMANA[diaSemana(l.data)]}</span></td>
        <td>${chip(TIPOS_OCORRENCIA[l.tipo], l.status === 'cancelado' ? 'muted' : TOM_OCORRENCIA[l.tipo])}</td>
        <td class="small">${l.qtdAulas == null ? '<span class="muted">Turno sem grade de horários</span>' : `<b>${plural(l.qtdAulas, 'aula perdida', 'aulas perdidas')}</b>`}${l.tipo !== 'falta' ? `<span class="sub">${horarioDoLancamento(l)}${l.observacao ? ` · ${esc(l.observacao)}` : ''}</span>` : ''}</td>
        <td>${l.status === 'cancelado' ? `${chip('Cancelado', 'muted', { semPonto: true })}<span class="sub">${esc(l.motivoCancelamento)}</span>` : chip('Válido', 'ok')}</td>
        <td class="small">${esc(l.autor)}<span class="sub">${fmtDataHora(l.criadoEm)}</span></td>
        <td><div class="row-actions">${l.status === 'ativo' && ehSecretaria() ? `<button class="btn btn-ghost btn-sm" type="button" data-acao="frequencia:cancelar" data-id="${l.id}">Cancelar</button>` : ''}</div></td>
      </tr>`),
  ) + botaoMostrarMais(chave, lista.length, limite, 60)
    : estadoVazio({ icone: 'calendar', titulo: 'Nenhum registro encontrado', texto: 'Sem faltas, atrasos ou saídas no período e tipo escolhidos.' })}`;
}

function abaContatos(p) {
  const { telefones, responsaveis } = p.contatos;
  return `<div class="card-b" style="display:grid;gap:16px">
    <div class="note">${icone('msg')}<span>A mensagem de falta vai primeiro para o telefone <b>Principal</b>. Sem resposta em ${PRAZO_RESPOSTA_HORAS} horas, vai para o próximo da lista, até o último. Use as setas para mudar a ordem.</span></div>
    <div class="card flat">
      <div class="card-h"><h3>Ordem de contato</h3><button class="btn btn-primary btn-sm" type="button" data-acao="contato:adicionar" data-aluno="${p.aluno.id}">${icone('plus', 15)}Adicionar telefone</button></div>
      ${tabela(['#', 'Telefone', 'Responsável', ''], telefones.map((t, i) => `<tr>
        <td><span class="contato-ordem ${i === 0 ? 'principal' : ''}">${i + 1}</span></td>
        <td><b class="num">${esc(t.numero)}</b><span class="sub">${esc(t.rotulo)}</span></td>
        <td>${esc(t.responsavel?.nome || '-')}<span class="sub">${esc(t.responsavel?.parentesco || '')}</span></td>
        <td><div class="row-actions">
          <button class="icon-btn" type="button" data-acao="contato:mover" data-id="${t.id}" data-direcao="subir" aria-label="Subir ${esc(t.numero)}" title="Subir" ${i === 0 ? 'disabled' : ''}>${icone('up')}</button>
          <button class="icon-btn" type="button" data-acao="contato:mover" data-id="${t.id}" data-direcao="descer" aria-label="Descer ${esc(t.numero)}" title="Descer" ${i === telefones.length - 1 ? 'disabled' : ''}>${icone('down')}</button>
          <button class="icon-btn" type="button" data-acao="contato:editar" data-id="${t.id}" data-aluno="${p.aluno.id}" aria-label="Editar ${esc(t.numero)}" title="Editar">${icone('edit')}</button>
          <button class="icon-btn" type="button" data-acao="contato:remover" data-id="${t.id}" aria-label="Remover ${esc(t.numero)}" title="Remover" ${telefones.length <= 1 ? 'disabled' : ''}>${icone('trash')}</button>
        </div></td>
      </tr>`))}
    </div>
    <div class="card flat">
      <div class="card-h"><h3>Responsáveis</h3></div>
      <div class="list">${responsaveis.map((r) => `<div class="list-item">
        <span class="avatar" style="background:var(--primary-soft);color:var(--primary-ink)">${iniciais(r.nome)}</span>
        <div class="grow"><div class="t">${esc(r.nome)}</div><div class="s">${esc(r.parentesco)} · ${plural(r.telefones.length, 'telefone', 'telefones')}: ${esc(r.telefones.map((t) => t.rotulo).join(', '))}</div></div>
        <div class="row-actions">
          <button class="icon-btn" type="button" data-acao="responsavel:editar" data-id="${r.id}" aria-label="Editar ${esc(r.nome)}" title="Editar">${icone('edit')}</button>
          <button class="icon-btn" type="button" data-acao="responsavel:remover" data-id="${r.id}" aria-label="Remover ${esc(r.nome)}" title="Remover" ${responsaveis.length <= 1 ? 'disabled' : ''}>${icone('trash')}</button>
        </div>
      </div>`).join('')}</div>
    </div>
  </div>`;
}

function abaMatriculas(p) {
  return `${tabela(
    ['Ano letivo', 'Turma', 'Escola', 'Período', 'Situação', 'Frequência', ''],
    p.matriculas.map(({ matricula: m, turma, escola, freq }) => `<tr>
      <td class="num"><b>${m.ano}</b></td>
      <td><a class="row-link" href="#/turmas/${turma.id}">${esc(turma.nome)}</a><span class="sub">${TURNOS[turma.turno]}</span></td>
      <td class="small">${esc(escola?.nome || '')}</td>
      <td class="num small">${fmtData(m.inicio)} a ${m.fim ? fmtData(m.fim) : 'atual'}</td>
      <td>${m.status === 'ativa' ? chip('Ativa', 'ok') : chip('Encerrada', 'muted', { semPonto: true })}</td>
      <td>${medidorFrequencia(freq)}</td>
      <td><div class="row-actions">${ehAdmin() && m.status === 'ativa' ? `<button class="btn btn-ghost btn-sm" type="button" data-acao="matricula:encerrar" data-id="${m.id}" data-inicio="${m.inicio}">Encerrar</button>` : ''}</div></td>
    </tr>`),
  )}
  <div class="tbl-foot">${p.ativa ? 'Um aluno só pode ter uma matrícula ativa por ano letivo. Para trocar de turma, encerre a matrícula atual e crie outra.'
    : ehAdmin() ? `<button class="btn btn-primary btn-sm" type="button" data-acao="matricula:nova" data-aluno="${p.aluno.id}">${icone('plus', 15)}Nova matrícula em ${p.cfg.anoAtual}</button>` : 'O aluno não tem matrícula ativa no ano letivo atual.'}</div>`;
}

function abaBuscaAtiva(p) {
  const aberto = p.casos.find((c) => c.status === 'aberto');
  if (!p.casos.length) {
    return estadoVazio({
      icone: 'compass', titulo: 'Nenhum caso de Busca Ativa', texto: 'A abertura de um caso é sempre uma decisão da secretaria.',
      acoes: p.ativa ? `<button class="btn btn-primary btn-sm" type="button" data-acao="caso:abrir" data-aluno="${p.aluno.id}">Abrir caso</button>` : '',
    });
  }
  return `<div class="list">${p.casos.map((c) => `<a class="list-item" href="#/busca-ativa/${c.id}">${icone('compass')}
    <div class="grow"><div class="t">Caso aberto em ${fmtData(c.abertura)}</div><div class="s">${c.status === 'encerrado' ? `Encerrado em ${fmtData(c.encerramento)}: ${esc(c.desfecho)}` : esc(c.motivo || 'Em andamento')}</div></div>
    ${c.status === 'aberto' ? chip('Em andamento', 'sun') : chip('Encerrado', 'muted', { semPonto: true })}
  </a>`).join('')}</div>
  ${!aberto && p.ativa ? `<div class="tbl-foot"><button class="btn btn-primary btn-sm" type="button" data-acao="caso:abrir" data-aluno="${p.aluno.id}">Abrir novo caso</button></div>` : ''}`;
}

async function telaAluno({ id }) {
  const p = await perfilDoAluno(id);
  const casoAberto = p.casos.find((c) => c.status === 'aberto');
  const turmaAtual = p.ativa?.turma;
  const lista = [
    ['historico', 'Histórico de frequência'],
    ['contatos', 'Responsáveis e telefones', p.contatos.telefones.length],
    ['matriculas', 'Matrículas', p.matriculas.length],
    ['notificacoes', 'Notificações', p.notificacoes.length],
    ...(ehSecretaria() ? [['busca', 'Busca Ativa', p.casos.length]] : []),
  ];
  const aba = abas('aluno', 'historico', lista);
  const conteudo = {
    historico: () => abaHistorico(p),
    contatos: () => abaContatos(p),
    matriculas: () => abaMatriculas(p),
    notificacoes: () => (p.notificacoes.length
      ? tabelaNotificacoes(p.notificacoes.map((n) => ({ notificacao: n, aluno: p.aluno, turma: null })), { comAluno: false })
      : estadoVazio({ icone: 'msg', titulo: 'Nenhuma mensagem enviada', texto: 'Quando houver falta registrada, o responsável recebe a mensagem no horário de envio do turno.' })),
    busca: () => abaBuscaAtiva(p),
  }[aba.atual]();

  return `${cabecalhoPagina({ trilha: [['Alunos', 'alunos'], [p.aluno.nome]], titulo: '' }).replace('<h1></h1>', '')}
  <section class="card"><div class="card-b profile-head">
    <div class="id">
      <span class="avatar">${iniciais(p.aluno.nome)}</span>
      <div style="min-width:0">
        <h1>${esc(p.aluno.nome)}</h1>
        <div class="chips" style="margin-top:4px">
          <span class="chip plain mono">RA ${esc(p.aluno.ra)}</span>
          ${turmaAtual ? chip(`${esc(turmaAtual.nome)} · ${TURNOS[turmaAtual.turno]}`, 'info', { semPonto: true }) : chip('Sem matrícula ativa', 'muted', { semPonto: true })}
          ${p.ativa?.freq.emAlerta ? chip('Em alerta de evasão', 'crit') : ''}
          ${casoAberto ? chip('Busca Ativa em andamento', 'sun') : ''}
        </div>
        ${ehAdmin() ? `<p class="small muted" style="margin-top:6px">${esc(p.escola.nome)}</p>` : ''}
      </div>
    </div>
    <div class="acoes">
      <button class="btn btn-ghost" type="button" data-acao="aluno:editar" data-id="${p.aluno.id}" data-ra="${esc(p.aluno.ra)}" data-nome="${esc(p.aluno.nome)}">${icone('edit')}Editar dados</button>
      ${ehSecretaria() ? (casoAberto ? `<a class="btn btn-sun" href="#/busca-ativa/${casoAberto.id}">${icone('compass')}Ver caso de Busca Ativa</a>`
    : p.ativa ? `<button class="btn btn-primary" type="button" data-acao="caso:abrir" data-aluno="${p.aluno.id}">${icone('compass')}Abrir caso de Busca Ativa</button>` : '') : ''}
    </div>
  </div></section>

  <div class="grid-2">
    <section class="card"><div class="card-h"><h2>Frequência no ano letivo ${p.cfg.anoAtual}</h2></div><div class="card-b">${cartaoFrequencia(p)}</div></section>
    <section class="card">
      <div class="card-h"><h2>Padrões de falta</h2><span class="small muted">Últimos 30 dias</span></div>
      <div class="card-b" style="display:grid;gap:10px">
        ${p.padroes.length ? p.padroes.map((x) => `<div class="pattern">${icone('calendar')}<span><b>${esc(x.titulo)}</b>${esc(x.detalhe)}</span></div>`).join('')
    : '<p class="muted">Nenhuma concentração de faltas nos últimos 30 dias.</p>'}
        <p class="small muted">Aparece quando o aluno falta 3 vezes ou mais no mesmo dia da semana.</p>
      </div>
    </section>
  </div>

  <section class="card">${aba.html}${conteudo}</section>`;
}

export const rotas = [
  { caminho: 'alunos', perfis: ['admin', 'secretaria'], tela: telaAlunos },
  { caminho: 'alunos/:id', perfis: ['admin', 'secretaria'], tela: telaAluno, menu: 'alunos' },
];

// ---------- Ações e formulários ----------

async function formularioNovoAluno(turmaId = '') {
  const { turmas, cfg, escolas } = await opcoesDeFiltro();
  const anoLetivo = cfg.anosLetivos[cfg.anoAtual];
  const nomesEscola = new Map(escolas.map((e) => [e.id, e.nome]));
  if (!turmas.length) { toast('Cadastre uma turma do ano letivo antes de cadastrar alunos.', 'crit'); return; }
  const inicioPadrao = hoje() < anoLetivo.inicio ? anoLetivo.inicio : hoje() > anoLetivo.fim ? anoLetivo.fim : hoje();
  abrirModal('Novo aluno', `<form data-form="aluno:novo" novalidate class="form-grid">
    ${campo({ rotulo: 'RA', nome: 'ra', obrigatorio: true, atributos: 'inputmode="numeric" placeholder="000123456789-0" maxlength="16"', dica: 'Registro do Aluno. Não pode repetir no sistema.' })}
    ${campo({ rotulo: 'Nome completo', nome: 'nome', obrigatorio: true, atributos: 'maxlength="120" autocomplete="off"' })}
    ${campo({ rotulo: `Turma (${cfg.anoAtual})`, nome: 'turmaId', tipo: 'select', obrigatorio: true, valor: turmaId, vazio: 'Selecione', opcoes: turmas.map((t) => [t.id, `${t.nome} · ${TURNOS[t.turno]} · ${nomesEscola.get(t.escolaId)?.replace(/^E\.E\.\s*/, '') || ''}`]) })}
    ${campo({ rotulo: 'Início da matrícula', nome: 'inicio', tipo: 'date', obrigatorio: true, valor: inicioPadrao, atributos: `min="${anoLetivo.inicio}" max="${anoLetivo.fim}"` })}
    <fieldset class="fieldset span-2"><legend>Responsável</legend><div class="form-grid">
      ${campo({ rotulo: 'Nome do responsável', nome: 'responsavelNome', obrigatorio: true, atributos: 'maxlength="120"' })}
      ${campo({ rotulo: 'Parentesco', nome: 'parentesco', tipo: 'select', obrigatorio: true, valor: 'Mãe', opcoes: PARENTESCOS.map((x) => [x, x]) })}
      ${campo({ rotulo: 'Telefone principal', nome: 'telefone1', obrigatorio: true, atributos: ATRIBUTOS_TELEFONE, dica: 'Recebe primeiro a mensagem de falta.' })}
      ${campo({ rotulo: 'Telefone secundário', nome: 'telefone2', atributos: ATRIBUTOS_TELEFONE, dica: `Usado se não houver resposta em ${PRAZO_RESPOSTA_HORAS} h. Opcional.` })}
    </div></fieldset>
    <p class="small muted span-2">Outros responsáveis e telefones podem ser adicionados depois, no perfil do aluno.</p>
    ${botoesModal('Cadastrar aluno')}
  </form>`, { largo: true });
}

async function opcoesResponsaveis(alunoId, selecionado, comNovo) {
  const { responsaveis } = await contatosDoAluno(alunoId);
  const opcoes = responsaveis.map((r) => [r.id, `${r.nome} (${r.parentesco})`]);
  if (comNovo) opcoes.push(['__novo', '+ Novo responsável']);
  return { opcoes, valor: selecionado || opcoes[0]?.[0] || '' };
}

registrarAcoes({
  'aluno:novo': (d) => formularioNovoAluno(d.turma || ''),

  'aluno:editar': (d) => abrirModal('Editar dados do aluno', `<form data-form="aluno:editar" novalidate class="form-grid">
    <input type="hidden" name="id" value="${esc(d.id)}">
    ${campo({ rotulo: 'RA', nome: 'ra', obrigatorio: true, valor: d.ra, atributos: 'inputmode="numeric" maxlength="16"' })}
    ${campo({ rotulo: 'Nome completo', nome: 'nome', obrigatorio: true, valor: d.nome, atributos: 'maxlength="120"' })}
    ${botoesModal('Salvar alterações')}
  </form>`),

  'aluno:exportarHistorico': async (d) => {
    const p = await perfilDoAluno(d.id);
    const f = estado(`hist:${d.id}`);
    const lista = filtrarHistorico(p, f);
    abrirExportacao({
      titulo: `Histórico de frequência · ${p.aluno.nome}`,
      filtros: `RA ${p.aluno.ra} · Ano ${f.ano} · Período: ${f.de ? fmtData(f.de) : 'início'} a ${f.ate ? fmtData(f.ate) : 'hoje'} · Tipo: ${f.tipo ? TIPOS_OCORRENCIA[f.tipo] : 'todos'} · Situação: ${f.situacao === 'ativo' ? 'válidos' : f.situacao === 'cancelado' ? 'cancelados' : 'todos'}`,
      colunas: ['Data', 'Tipo', 'Horário', 'Aulas perdidas', 'Motivo', 'Situação', 'Motivo do cancelamento'],
      linhas: lista.map((l) => [fmtData(l.data), TIPOS_OCORRENCIA[l.tipo], horarioDoLancamento(l), l.qtdAulas ?? '', l.observacao || '', l.status === 'ativo' ? 'Válido' : 'Cancelado', l.motivoCancelamento || '']),
      nomeArquivo: `historico-${p.aluno.ra}-${f.ano}`,
    });
  },

  'contato:adicionar': async (d) => {
    const { opcoes, valor } = await opcoesResponsaveis(d.aluno, '', true);
    abrirModal('Adicionar telefone', `<form data-form="contato:adicionar" novalidate class="form-grid">
      <input type="hidden" name="alunoId" value="${esc(d.aluno)}">
      ${campo({ rotulo: 'Responsável', nome: 'responsavelId', tipo: 'select', obrigatorio: true, largo: true, valor, opcoes, atributos: 'data-alterna-alvo="#novo-responsavel" data-alterna-valor="__novo"' })}
      <fieldset class="fieldset span-2" id="novo-responsavel" ${valor === '__novo' ? '' : 'hidden'}><legend>Novo responsável</legend><div class="form-grid">
        ${campo({ rotulo: 'Nome', nome: 'novoNome', obrigatorio: true, atributos: 'maxlength="120"' })}
        ${campo({ rotulo: 'Parentesco', nome: 'novoParentesco', tipo: 'select', obrigatorio: true, valor: 'Mãe', opcoes: PARENTESCOS.map((x) => [x, x]) })}
      </div></fieldset>
      ${campo({ rotulo: 'Telefone', nome: 'numero', obrigatorio: true, largo: true, atributos: ATRIBUTOS_TELEFONE, dica: 'O telefone entra no fim da lista. Depois é possível mudar a ordem com as setas.' })}
      ${botoesModal('Adicionar telefone')}
    </form>`);
  },

  'contato:editar': async (d) => {
    const telefone = await api.telefones.obter(d.id);
    const { opcoes } = await opcoesResponsaveis(d.aluno, telefone.responsavelId, false);
    abrirModal('Editar telefone', `<form data-form="contato:editar" novalidate class="form-grid one">
      <input type="hidden" name="id" value="${esc(d.id)}">
      ${campo({ rotulo: 'Telefone', nome: 'numero', obrigatorio: true, valor: telefone.numero, atributos: ATRIBUTOS_TELEFONE })}
      ${campo({ rotulo: 'Responsável', nome: 'responsavelId', tipo: 'select', obrigatorio: true, valor: telefone.responsavelId, opcoes })}
      ${botoesModal('Salvar alterações', { largo: false })}
    </form>`);
  },

  'contato:mover': async (d) => { await moverTelefone(d.id, d.direcao); atualizar(); },

  'contato:remover': async (d) => {
    const telefone = await api.telefones.obter(d.id);
    confirmar({
      titulo: 'Remover telefone?',
      texto: `<p>O número <b>${esc(telefone.numero)}</b> sai da ordem de contato e deixa de receber mensagens. Os demais sobem uma posição.</p>`,
      botao: 'Remover',
      aoConfirmar: async () => { await removerTelefone(d.id); atualizar(); toast('Telefone removido.'); },
    });
  },

  'responsavel:editar': async (d) => {
    const r = await api.responsaveis.obter(d.id);
    abrirModal('Editar responsável', `<form data-form="responsavel:editar" novalidate class="form-grid">
      <input type="hidden" name="id" value="${esc(r.id)}">
      ${campo({ rotulo: 'Nome', nome: 'nome', obrigatorio: true, valor: r.nome, atributos: 'maxlength="120"' })}
      ${campo({ rotulo: 'Parentesco', nome: 'parentesco', tipo: 'select', obrigatorio: true, valor: r.parentesco, opcoes: PARENTESCOS.map((x) => [x, x]) })}
      ${botoesModal('Salvar alterações')}
    </form>`);
  },

  'responsavel:remover': async (d) => {
    const r = await api.responsaveis.obter(d.id);
    confirmar({
      titulo: 'Remover responsável?',
      texto: `<p><b>${esc(r.nome)}</b> e os telefones dele saem da ordem de contato e deixam de receber mensagens sobre o aluno.</p>`,
      botao: 'Remover',
      aoConfirmar: async () => { await removerResponsavel(d.id); atualizar(); toast('Responsável removido.'); },
    });
  },

  'matricula:nova': async (d) => {
    const { turmas, cfg } = await opcoesDeFiltro();
    const anoLetivo = cfg.anosLetivos[cfg.anoAtual];
    abrirModal('Nova matrícula', `<form data-form="matricula:nova" novalidate class="form-grid">
      <input type="hidden" name="alunoId" value="${esc(d.aluno)}">
      ${campo({ rotulo: `Turma (${cfg.anoAtual})`, nome: 'turmaId', tipo: 'select', obrigatorio: true, vazio: 'Selecione', opcoes: turmas.sort(porNome).map((t) => [t.id, `${t.nome} · ${TURNOS[t.turno]}`]) })}
      ${campo({ rotulo: 'Início da matrícula', nome: 'inicio', tipo: 'date', obrigatorio: true, valor: hoje(), atributos: `min="${anoLetivo.inicio}" max="${anoLetivo.fim}"` })}
      ${botoesModal('Matricular')}
    </form>`);
  },

  'matricula:encerrar': (d) => abrirModal('Encerrar matrícula', `<form data-form="matricula:encerrar" novalidate class="form-grid one">
    <input type="hidden" name="id" value="${esc(d.id)}">
    <p>Os registros de frequência e o histórico continuam guardados.</p>
    ${campo({ rotulo: 'Data de encerramento', nome: 'fim', tipo: 'date', obrigatorio: true, valor: hoje(), atributos: `min="${esc(d.inicio)}"` })}
    ${botoesModal('Encerrar matrícula', { tom: 'danger', largo: false })}
  </form>`),
});

registrarFormularios({
  'aluno:novo': async (dados) => {
    const aluno = await criarAluno(dados);
    fecharModal();
    navegar(`alunos/${aluno.id}`);
    toast('Aluno cadastrado e matriculado.');
  },
  'aluno:editar': async (dados) => { await editarAluno(dados.id, dados); fecharModal(); atualizar(); toast('Dados do aluno atualizados.'); },
  'contato:adicionar': async (dados) => { await adicionarTelefone(dados); fecharModal(); atualizar(); toast('Telefone adicionado ao fim da ordem de contato.'); },
  'contato:editar': async (dados) => { await editarTelefone(dados.id, dados); fecharModal(); atualizar(); toast('Telefone atualizado.'); },
  'responsavel:editar': async (dados) => { await editarResponsavel(dados.id, dados); fecharModal(); atualizar(); toast('Responsável atualizado.'); },
  'matricula:nova': async (dados) => { await matricular(dados); fecharModal(); atualizar(); toast('Matrícula criada.'); },
  'matricula:encerrar': async (dados) => { await encerrarMatricula(dados.id, dados.fim); fecharModal(); atualizar(); toast('Matrícula encerrada. O histórico foi mantido.'); },
});

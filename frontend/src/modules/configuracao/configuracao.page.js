// Tela "Turnos e envio": horário das mensagens de falta e grade de horários das aulas,
// em cada turno do modelo da escola. Administrador escolhe a escola; a secretaria vê só a dela.

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { ehAdmin, usuarioAtual } from '../../app/session.js';
import { TURNOS, MODELOS_TURNO } from '../../app/constantes.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { filtroSelect } from '../../components/filters.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { confirmar } from '../../components/confirm.js';
import { campo, botoesModal } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc, plural } from '../../utils/texto.js';
import { DIAS_SEMANA, fmtData, hoje, somarDias } from '../../utils/datas.js';
import { porNome } from '../../utils/listas.js';
import { configuracaoDaEscola, salvarTurno, cancelarMudanca, periodoDoDia, nomeDoTurno, DIAS_LETIVOS } from './configuracao.service.js';

const SIGLA = { 1: 'Seg', 2: 'Ter', 3: 'Qua', 4: 'Qui', 5: 'Sex' };

function situacao(t) {
  if (!t.versoes && !t.envio) return t.turmas ? chip('Não configurado', 'crit') : chip('Não usado', 'muted', { semPonto: true });
  if (!t.versoes) return chip('Falta a grade', 'crit');
  if (!t.envio) return chip('Sem envio', 'warn');
  return chip('Configurado', 'ok');
}

/** Lista compacta da grade: um dia por linha, com quantidade e horário. */
function tabelaGrade(versao) {
  return `<div class="grade-resumo">${DIAS_LETIVOS.map((d) => {
    const aulas = versao.dias[d];
    const p = periodoDoDia(aulas);
    return `<details class="grade-dia">
      <summary><b>${SIGLA[d]}</b><span>${aulas.length ? plural(aulas.length, 'aula', 'aulas') : 'Sem aula'}</span><span class="muted">${p ? `${p.inicio} às ${p.fim}` : ''}</span></summary>
      ${aulas.length ? `<ol class="grade-aulas">${aulas.map((a) => `<li><b>${a.ordem}ª</b> ${a.inicio} às ${a.fim}</li>`).join('')}</ol>` : ''}
    </details>`;
  }).join('')}</div>`;
}

const resumoCurto = (versao) => DIAS_LETIVOS.map((d) => `${SIGLA[d]} ${versao.aulas[d - 1]}`).join(' · ');

function cartaoTurno(t, escolaId) {
  const avisos = [];
  if (t.turmas && !t.versoes) avisos.push(`<div class="note crit">${icone('alert')}<span>Sem a grade de horários, não dá para lançar ocorrências nem calcular a frequência das turmas deste turno.</span></div>`);
  if (t.turmas && !t.envio) avisos.push(`<div class="note sun">${icone('msg')}<span>Sem horário de envio, os responsáveis deste turno não recebem a mensagem de falta.</span></div>`);
  const futuras = t.futuras.map((v) => `<div class="note">${icone('calendar')}<span><b>Grade nova a partir de ${fmtData(v.vigencia)}:</b> ${resumoCurto(v)} aulas.
    <button class="btn btn-ghost btn-sm" type="button" style="margin-top:6px" data-acao="turno:cancelarMudanca" data-escola="${escolaId}" data-turno="${t.turno}" data-vigencia="${v.vigencia}">Cancelar esta mudança</button></span></div>`).join('');
  const anteriores = t.anteriores.length ? `<details class="small"><summary class="muted">Grades anteriores (${t.anteriores.length})</summary>
    <ul class="versoes-lista">${t.anteriores.map((v) => `<li>Desde ${fmtData(v.vigencia)}: ${resumoCurto(v)} aulas</li>`).join('')}</ul></details>` : '';
  return `<section class="card turno-card">
    <div class="card-h"><h2>${TURNOS[t.turno]}</h2>${situacao(t)}</div>
    <div class="card-b" style="display:grid;gap:16px">
      <dl class="dl">
        <dt>Envio das mensagens</dt><dd>${t.envio ? `<b class="num" style="font-size:18px">${t.envio}</b>` : '<span class="muted">Não definido</span>'}</dd>
        <dt>Turmas no ano</dt><dd>${t.turmas}</dd>
      </dl>
      <div>
        <div class="eyebrow" style="margin-bottom:8px">Grade de horários${t.atual ? ` · em vigor desde ${fmtData(t.atual.vigencia)}` : ''}</div>
        ${t.atual ? `${tabelaGrade(t.atual)}<p class="small muted" style="margin-top:8px">${plural(t.total, 'aula', 'aulas')} por semana. Clique num dia para ver os horários.</p>`
    : `<p class="muted small">${t.versoes ? 'Ainda não começou a valer.' : 'Não configurada.'}</p>`}
      </div>
      ${futuras}
      ${anteriores}
      ${avisos.join('')}
      <div><button class="btn btn-ghost btn-sm" type="button" data-acao="turno:editar" data-escola="${escolaId}" data-turno="${t.turno}">${icone('edit', 15)}Editar turno</button></div>
    </div>
  </section>`;
}

async function telaTurnos() {
  const escolas = (await api.escolas.listar()).sort(porNome);
  const f = estado('turnos', { escola: '' });
  let escolaId = usuarioAtual().escolaId;
  if (ehAdmin()) {
    if (!escolas.some((e) => e.id === f.escola)) f.escola = escolas[0]?.id || '';
    escolaId = f.escola;
  }
  const cabecalho = cabecalhoPagina({
    titulo: 'Turnos e envio',
    subtitulo: 'Em cada turno, o horário em que as mensagens de falta saem e a grade de horários das aulas de cada dia. Uma mudança na grade vale a partir da data escolhida: os dias que já passaram não mudam.',
  });
  if (!escolaId) {
    return `${cabecalho}<section class="card">${estadoVazio({ icone: 'school', titulo: 'Nenhuma escola cadastrada', texto: 'Cadastre uma escola antes de configurar os turnos.', acoes: '<a class="btn btn-primary btn-sm" href="#/escolas">Ir para escolas</a>' })}</section>`;
  }
  const { escola, turnos } = await configuracaoDaEscola(escolaId);
  const modelo = `<span class="small muted">Modelo de turno: <b>${MODELOS_TURNO[escola.modeloTurno || 'parcial']}</b>${ehAdmin() ? ' (muda no cadastro da escola)' : ''}</span>`;
  return `${cabecalho}
  ${ehAdmin() ? `<section class="card" style="margin-bottom:18px"><div class="filters">
    ${filtroSelect({ rotulo: 'Escola', chave: 'turnos.escola', valor: f.escola, opcoes: escolas.map((e) => [e.id, e.nome]), todas: null, largo: true })}
    <div class="filter" style="max-width:none;align-self:center">${modelo}</div>
  </div></section>` : `<p class="small muted" style="margin-bottom:14px">${esc(escola.nome)} · ${modelo}</p>`}
  <div class="turnos-grid ${turnos.length === 1 ? 'um' : ''}">${turnos.map((t) => cartaoTurno(t, escolaId)).join('')}</div>`;
}

export const rotas = [{ caminho: 'turnos', perfis: ['admin', 'secretaria'], tela: telaTurnos }];

// ---------- Formulário da grade ----------

let proximaLinha = 0;

function linhaAula(dia, aula = { inicio: '', fim: '' }) {
  const k = proximaLinha++;
  return `<div class="field aula-linha">
    <span class="n"></span>
    <input type="time" id="trn_d${dia}_${k}_ini" name="d${dia}_${k}_ini" value="${aula.inicio}" aria-label="${DIAS_SEMANA[dia]}: início da aula">
    <span class="muted">às</span>
    <input type="time" id="trn_d${dia}_${k}_fim" name="d${dia}_${k}_fim" value="${aula.fim}" aria-label="${DIAS_SEMANA[dia]}: término da aula">
    <button class="icon-btn" type="button" data-acao="grade:remover" aria-label="Remover esta aula" title="Remover">${icone('x', 15)}</button>
  </div>`;
}

function renumerar(lista) {
  lista.querySelectorAll('.aula-linha').forEach((l, i) => { l.querySelector('.n').textContent = `${i + 1}ª`; });
  const vazio = lista.parentElement.querySelector('.dia-vazio');
  if (vazio) vazio.hidden = lista.children.length > 0;
}

function blocoDia(dia, aulas) {
  const outros = DIAS_LETIVOS.filter((d) => d !== dia);
  return `<fieldset class="fieldset grade-edit-dia">
    <legend>${DIAS_SEMANA[dia]}</legend>
    <div class="aulas-lista" id="grade-dia-${dia}">${aulas.map((a) => linhaAula(dia, a)).join('')}</div>
    <p class="small muted dia-vazio" ${aulas.length ? 'hidden' : ''}>Sem aula neste dia.</p>
    <div class="grade-edit-acoes">
      <button class="btn btn-ghost btn-sm" type="button" data-acao="grade:adicionar" data-dia="${dia}">${icone('plus', 15)}Adicionar aula</button>
      <span class="copiar"><label class="small muted" for="copiar_${dia}">Copiar de</label>
        <select id="copiar_${dia}">${outros.map((d) => `<option value="${d}">${DIAS_SEMANA[d]}</option>`).join('')}</select>
        <button class="btn btn-ghost btn-sm" type="button" data-acao="grade:copiar" data-dia="${dia}">Copiar</button></span>
    </div>
  </fieldset>`;
}

registrarAcoes({
  'turno:editar': async (d) => {
    const { escola, turnos, inicioAno } = await configuracaoDaEscola(d.escola);
    const t = turnos.find((x) => x.turno === d.turno);
    const base = t.futuras[t.futuras.length - 1] || t.atual || (t.versoes ? t.versoes[t.versoes.length - 1] : null);
    const amanha = somarDias(hoje(), 1);
    abrirModal(`${TURNOS[t.turno]}: envio e grade de horários`, `<form data-form="turno:salvar" novalidate class="form-grid one">
      <input type="hidden" name="escolaId" value="${esc(escola.id)}">
      <input type="hidden" name="turno" value="${t.turno}">
      <p class="small muted">${esc(escola.nome)}</p>
      ${campo({ prefixo: 'trn', rotulo: 'Horário de envio das mensagens', nome: 'envio', tipo: 'time', valor: t.envio || '', dica: 'Deixe vazio para não enviar mensagens neste turno. Se um aluno com falta chegar antes deste horário, a secretaria usa "Chegou atrasado" e a mensagem não sai.' })}
      <div class="note">${icone('info')}<span>Cadastre só as aulas. Intervalo, almoço e tutoria ficam de fora: são o espaço entre uma aula e outra. A ordem (1ª, 2ª...) segue o horário de início.</span></div>
      <div class="grade-edit">${DIAS_LETIVOS.map((dia) => blocoDia(dia, base ? base.dias[dia] : [])).join('')}</div>
      ${t.versoes
    ? campo({ prefixo: 'trn', rotulo: 'Se mudar a grade, ela vale a partir de', nome: 'vigencia', tipo: 'date', valor: amanha, atributos: `min="${hoje()}"`, dica: 'Os dias anteriores continuam com a grade que valia neles, então a frequência já calculada não muda. Se só o horário de envio mudou, esta data é ignorada.' })
    : `<p class="hint">A primeira grade vale desde o início do ano letivo (${fmtData(inicioAno)}). Pode deixar em branco e montar depois.</p>`}
      ${botoesModal('Salvar turno', { largo: false })}
    </form>`, { largo: true });
    document.querySelectorAll('.aulas-lista').forEach(renumerar);
  },
  'grade:adicionar': (d) => {
    const lista = document.getElementById(`grade-dia-${d.dia}`);
    const ultima = [...lista.querySelectorAll('input[name$="_fim"]')].pop()?.value || '';
    lista.insertAdjacentHTML('beforeend', linhaAula(Number(d.dia), { inicio: ultima, fim: '' }));
    renumerar(lista);
    lista.lastElementChild.querySelector(ultima ? 'input[name$="_fim"]' : 'input').focus();
  },
  'grade:remover': (_d, botao) => {
    const lista = botao.closest('.aulas-lista');
    botao.closest('.aula-linha').remove();
    renumerar(lista);
  },
  'grade:copiar': (d) => {
    const destino = Number(d.dia);
    const origem = Number(document.getElementById(`copiar_${destino}`).value);
    const linhas = [...document.querySelectorAll(`#grade-dia-${origem} .aula-linha`)].map((l) => ({
      inicio: l.querySelector('input[name$="_ini"]').value,
      fim: l.querySelector('input[name$="_fim"]').value,
    })).filter((a) => a.inicio || a.fim);
    const lista = document.getElementById(`grade-dia-${destino}`);
    lista.innerHTML = linhas.map((a) => linhaAula(destino, a)).join('');
    renumerar(lista);
    toast(`${plural(linhas.length, 'aula copiada', 'aulas copiadas')} de ${DIAS_SEMANA[origem].toLowerCase()}.`);
  },
  'turno:cancelarMudanca': (d) => confirmar({
    titulo: 'Cancelar esta mudança?',
    texto: `<p>A grade nova do ${nomeDoTurno(d.turno)} marcada para ${fmtData(d.vigencia)} ainda não começou a valer e será descartada.</p>`,
    botao: 'Cancelar mudança',
    aoConfirmar: async () => { await cancelarMudanca({ escolaId: d.escola, turno: d.turno, vigencia: d.vigencia }); atualizar(); toast('Mudança cancelada.'); },
  }),
});

const comMaiuscula = (t) => `${t[0].toUpperCase()}${t.slice(1)}`;

registrarFormularios({
  'turno:salvar': async (dados) => {
    const r = await salvarTurno(dados);
    fecharModal();
    atualizar();
    const nome = comMaiuscula(nomeDoTurno(dados.turno));
    toast(r.criouVersao && !r.primeira ? `${nome} salvo. A grade nova vale a partir de ${fmtData(r.vigencia)}.` : `${nome} salvo.`);
  },
});

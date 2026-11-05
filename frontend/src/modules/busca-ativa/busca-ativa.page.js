// Telas de Busca Ativa Escolar (somente secretaria).

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar, navegar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { TURNOS, CANAIS_CONTATO, RESULTADOS_CONTATO, TIPOS_ACAO, DESFECHOS_CASO } from '../../app/constantes.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { medidorFrequencia } from '../../components/frequency-meter.js';
import { filtroBusca } from '../../components/filters.js';
import { abas } from '../../components/tabs.js';
import { tabela } from '../../components/table.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { abrirExportacao } from '../../components/export.js';
import { campo, botoesModal } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc } from '../../utils/texto.js';
import { hoje, horaAgora, fmtData, fmtDataHora, diasEntre } from '../../utils/datas.js';
import { fmtPct } from '../../utils/formatos.js';
import {
  listarCasos, obterCaso, candidatosACaso, casoAbertoDoAluno, abrirCaso, registrarTentativa, registrarAcao, encerrarCaso,
} from './busca-ativa.service.js';

async function telaCasos() {
  const f = estado('busca', { q: '' });
  const atual = estado('abas').busca || 'aberto';
  const { contagem, linhas } = await listarCasos({ status: atual, q: f.q });
  const aba = abas('busca', 'aberto', [['aberto', 'Em andamento', contagem.aberto], ['encerrado', 'Encerrados', contagem.encerrado]]);
  const emAndamento = aba.atual === 'aberto';

  return `${cabecalhoPagina({
    titulo: 'Busca Ativa Escolar',
    subtitulo: 'Acompanhamento dos alunos escolhidos pela secretaria: tentativas de contato com a família, ações combinadas e desfecho.',
    acoes: `<button class="btn btn-primary" type="button" data-acao="caso:abrir">${icone('plus')}Abrir caso</button>`,
  })}
  <section class="card">
    ${aba.html}
    <div class="filters">${filtroBusca({ rotulo: 'Aluno', chave: 'busca.q', valor: f.q, placeholder: 'Buscar pelo nome' })}</div>
    ${linhas.length ? tabela(
    ['Aluno', 'Frequência', 'Abertura', { texto: 'Tentativas', classe: 'r' }, 'Último contato', emAndamento ? 'Dias em aberto' : 'Desfecho'],
    linhas.map((l) => `<tr>
        <td><a class="row-link" href="#/busca-ativa/${l.caso.id}">${esc(l.aluno.nome)}</a><span class="sub">${l.turma ? `${esc(l.turma.nome)} · ${TURNOS[l.turma.turno]}` : 'Sem matrícula ativa'}</span></td>
        <td>${l.freq ? medidorFrequencia(l.freq) : '<span class="muted">-</span>'}</td>
        <td class="num">${fmtData(l.caso.abertura)}</td>
        <td class="r num">${l.tentativas}</td>
        <td class="small">${l.ultimaTentativa ? `${esc(l.ultimaTentativa.resultado)}<span class="sub">${fmtDataHora(l.ultimaTentativa.dataHora)}</span>` : '<span class="muted">Nenhum ainda</span>'}</td>
        <td>${emAndamento ? `<span class="num">${Math.max(0, diasEntre(l.caso.abertura, hoje()))}</span>` : `<span class="small">${esc(l.caso.desfecho)}</span>`}</td>
      </tr>`),
  ) : estadoVazio({
    icone: 'compass',
    titulo: emAndamento ? 'Nenhum caso em andamento' : 'Nenhum caso encerrado',
    texto: f.q ? 'Ninguém com esse nome nesta lista.' : emAndamento ? 'Abra um caso para registrar os contatos e as ações com a família. O painel de alerta ajuda a decidir quem acompanhar.' : 'Os casos encerrados ficam aqui para consulta.',
    acoes: emAndamento && !f.q ? '<a class="btn btn-ghost btn-sm" href="#/alertas">Ver alerta de evasão</a><button class="btn btn-primary btn-sm" type="button" data-acao="caso:abrir">Abrir caso</button>' : '',
  })}
  </section>`;
}

async function telaCaso({ id }) {
  const { caso, aluno, turma, freq, contatos, responsaveis, tentativas, acoes } = await obterCaso(id);
  const aberto = caso.status === 'aberto';
  const agora = `${hoje()}T${horaAgora()}`;
  const tomTentativa = (r) => (r === 'Contato efetivo' ? 'ok' : r === 'Sem sucesso' || r === 'Número inválido' ? 'crit' : '');
  const iconeCanal = (c) => ({ WhatsApp: 'msg', 'Ligação telefônica': 'phone', 'Visita domiciliar': 'home', 'Reunião na escola': 'users' }[c] || 'file');

  return `${cabecalhoPagina({
    trilha: [['Busca Ativa', 'busca-ativa'], [aluno.nome]],
    sobretitulo: `Caso aberto em ${fmtData(caso.abertura)}`,
    titulo: esc(aluno.nome),
    subtitulo: caso.motivo ? esc(caso.motivo) : '',
    acoes: `${aberto ? chip('Em andamento', 'sun') : chip('Encerrado', 'muted', { semPonto: true })}
      <button class="btn btn-ghost" type="button" data-acao="caso:exportar" data-id="${esc(caso.id)}">${icone('download')}Exportar</button>
      <a class="btn btn-ghost" href="#/alunos/${aluno.id}">Ver aluno</a>
      ${aberto ? `<button class="btn btn-danger" type="button" data-acao="caso:encerrar" data-id="${caso.id}" data-abertura="${caso.abertura}">Encerrar caso</button>` : ''}`,
  })}
  ${!aberto ? `<div class="note sun">${icone('lock')}<span><b>Caso encerrado em ${fmtData(caso.encerramento)}: ${esc(caso.desfecho)}.</b> ${esc(caso.observacaoFinal || '')} Disponível apenas para consulta.</span></div>` : ''}
  <div class="grid-3-1">
    <section class="card">
      <div class="card-h"><h2>Tentativas de contato</h2>${chip(String(tentativas.length), '', { semPonto: true })}</div>
      ${tentativas.length ? `<div class="timeline">${tentativas.map((t) => `<div class="tl-item ${tomTentativa(t.resultado)}">
        <span class="tl-dot">${icone(iconeCanal(t.canal))}</span>
        <div><div class="when">${fmtDataHora(t.dataHora)} · ${esc(t.canal)}${t.responsavel ? ` · com ${esc(t.responsavel.nome)} (${esc(t.responsavel.parentesco)})` : ''}</div><div class="what">${esc(t.resultado)}</div><div class="body">${esc(t.descricao)}</div></div>
      </div>`).join('')}</div>` : '<div class="card-b muted">Nenhuma tentativa registrada ainda.</div>'}
      ${aberto ? `<div class="card-b card-sep"><form data-form="caso:tentativa" novalidate class="form-grid">
        <input type="hidden" name="casoId" value="${caso.id}">
        <h3 class="span-2" style="font-size:15px">Registrar tentativa</h3>
        ${campo({ prefixo: 'tnt', rotulo: 'Canal', nome: 'canal', tipo: 'select', obrigatorio: true, valor: 'Ligação telefônica', opcoes: CANAIS_CONTATO.map((x) => [x, x]) })}
        ${campo({ prefixo: 'tnt', rotulo: 'Data e hora', nome: 'dataHora', tipo: 'datetime-local', obrigatorio: true, valor: agora, atributos: `max="${agora}"` })}
        ${campo({ prefixo: 'tnt', rotulo: 'Responsável contatado', nome: 'responsavelId', tipo: 'select', vazio: 'Não informado', opcoes: responsaveis.map((r) => [r.id, `${r.nome} (${r.parentesco})`]) })}
        ${campo({ prefixo: 'tnt', rotulo: 'Resultado', nome: 'resultado', tipo: 'select', obrigatorio: true, vazio: 'Selecione', opcoes: RESULTADOS_CONTATO.map((x) => [x, x]) })}
        ${campo({ prefixo: 'tnt', rotulo: 'Resumo da conversa', nome: 'descricao', tipo: 'textarea', obrigatorio: true, largo: true, atributos: 'maxlength="1000" placeholder="O que foi conversado e combinado"' })}
        <div class="field span-2" style="justify-content:flex-end;display:flex"><button class="btn btn-primary" type="submit">Registrar tentativa</button></div>
      </form></div>` : ''}
    </section>

    <div class="stack">
      <section class="card"><div class="card-h"><h2>Aluno</h2></div><div class="card-b"><dl class="dl">
        <dt>Turma</dt><dd>${turma ? `${esc(turma.nome)} · ${TURNOS[turma.turno]}` : 'Sem matrícula ativa'}</dd>
        <dt>Frequência</dt><dd>${freq ? `${medidorFrequencia(freq)}` : '-'}</dd>
        <dt>Contatos</dt><dd>${contatos.map((c) => `${esc(c.rotulo)}: ${esc(c.nome)} (${esc(c.parentesco)})<br><span class="num small">${esc(c.numero)}</span>`).join('<br>') || '<span class="muted">Nenhum</span>'}</dd>
      </dl></div></section>

      <section class="card">
        <div class="card-h"><h2>Ações e encaminhamentos</h2>${chip(String(acoes.length), '', { semPonto: true })}</div>
        ${acoes.length ? `<div class="list">${acoes.map((a) => `<div class="list-item" style="align-items:flex-start">${icone('flag')}
          <div class="grow"><div class="t">${esc(a.tipoAcao)}</div><div class="s">${fmtDataHora(a.dataHora)}</div><div style="font-size:14px;margin-top:2px">${esc(a.descricao)}</div></div>
        </div>`).join('')}</div>` : '<div class="card-b muted small">Nenhuma ação registrada.</div>'}
        ${aberto ? `<div class="card-b card-sep"><form data-form="caso:acao" novalidate class="form-grid one">
          <input type="hidden" name="casoId" value="${caso.id}">
          ${campo({ prefixo: 'aca', rotulo: 'Tipo', nome: 'tipoAcao', tipo: 'select', obrigatorio: true, vazio: 'Selecione', opcoes: TIPOS_ACAO.map((x) => [x, x]) })}
          ${campo({ prefixo: 'aca', rotulo: 'Data e hora', nome: 'dataHora', tipo: 'datetime-local', obrigatorio: true, valor: agora, atributos: `min="${caso.abertura}T00:00" max="${agora}"` })}
          ${campo({ prefixo: 'aca', rotulo: 'Descrição', nome: 'descricao', tipo: 'textarea', obrigatorio: true, atributos: 'maxlength="1000"' })}
          <button class="btn btn-primary" type="submit">Registrar ação</button>
        </form></div>` : ''}
      </section>
    </div>
  </div>`;
}

export const rotas = [
  { caminho: 'busca-ativa', perfis: ['secretaria'], tela: telaCasos },
  { caminho: 'busca-ativa/:id', perfis: ['secretaria'], tela: telaCaso, menu: 'busca-ativa' },
];

registrarAcoes({
  'caso:exportar': async (d) => {
    const { caso, aluno, turma, freq, contatos, tentativas, acoes } = await obterCaso(d.id);
    const registros = [
      ...tentativas.map((t) => ({
        data: t.dataHora,
        linha: ['Tentativa de contato', fmtDataHora(t.dataHora), t.canal, t.resultado, [t.responsavel ? `Com ${t.responsavel.nome} (${t.responsavel.parentesco})` : '', t.descricao].filter(Boolean).join(' · ')],
      })),
      ...acoes.map((a) => ({
        data: a.dataHora,
        linha: ['Ação/encaminhamento', fmtDataHora(a.dataHora), a.tipoAcao, '', a.descricao],
      })),
    ].sort((a, b) => a.data.localeCompare(b.data));
    const linhas = [
      ['Aluno', '', 'RA', '', aluno.ra],
      ['Matrícula', '', 'Turma', '', turma ? `${turma.nome} · ${TURNOS[turma.turno]}` : 'Sem matrícula ativa'],
      ['Frequência', '', 'Percentual', '', freq?.pct == null ? 'Sem dados de frequência' : fmtPct(freq.pct)],
      ['Caso', fmtData(caso.abertura), caso.status === 'aberto' ? 'Em andamento' : 'Encerrado', caso.desfecho || '', [
        caso.motivo ? `Motivo: ${caso.motivo}` : '',
        caso.encerramento ? `Encerrado em ${fmtData(caso.encerramento)}` : '',
        caso.observacaoFinal || '',
      ].filter(Boolean).join(' · ') || '-'],
      ...contatos.map((c) => ['Contato familiar', '', c.rotulo, c.parentesco, `${c.nome} · ${c.numero}`]),
      ...registros.map((registro) => registro.linha),
    ];
    abrirExportacao({
      titulo: `Relatório de Busca Ativa · ${aluno.nome}`,
      filtros: `Caso ${caso.status === 'aberto' ? 'em andamento' : 'encerrado'} · Abertura: ${fmtData(caso.abertura)} · ${tentativas.length} ${tentativas.length === 1 ? 'tentativa' : 'tentativas'} · ${acoes.length} ${acoes.length === 1 ? 'ação' : 'ações'}`,
      colunas: ['Registro', 'Data', 'Canal/tipo', 'Resultado/desfecho', 'Detalhes'],
      linhas,
      nomeArquivo: `busca-ativa-${aluno.ra}`,
    });
  },
  'caso:abrir': async (d) => {
    if (d.aluno) {
      const existente = await casoAbertoDoAluno(d.aluno);
      if (existente) { navegar(`busca-ativa/${existente.id}`); return; }
    }
    const candidatos = await candidatosACaso();
    abrirModal('Abrir caso de Busca Ativa', `<form data-form="caso:abrir" novalidate class="form-grid one">
      <div class="note">${icone('info')}<span>Cada aluno pode ter só um caso aberto por vez. O aluno não precisa estar no alerta de evasão.</span></div>
      ${campo({ rotulo: 'Aluno', nome: 'alunoId', tipo: 'select', obrigatorio: true, valor: d.aluno || '', vazio: 'Selecione', opcoes: candidatos.map((c) => [c.aluno.id, `${c.aluno.nome} · ${fmtPct(c.freq.pct)}${c.freq.emAlerta ? ' · em alerta' : ''}`]), dica: 'Da menor para a maior frequência. Alunos com caso aberto não aparecem.' })}
      ${campo({ rotulo: 'Data de abertura', nome: 'abertura', tipo: 'date', obrigatorio: true, valor: hoje(), atributos: `max="${hoje()}"` })}
      ${campo({ rotulo: 'Motivo da abertura', nome: 'motivo', tipo: 'textarea', atributos: 'maxlength="500" placeholder="Opcional. Exemplo: faltas seguidas sem justificativa"' })}
      ${botoesModal('Abrir caso', { largo: false })}
    </form>`);
  },
  'caso:encerrar': (d) => abrirModal('Encerrar caso', `<form data-form="caso:encerrar" novalidate class="form-grid one">
    <input type="hidden" name="id" value="${esc(d.id)}">
    <div class="note sun">${icone('lock')}<span>Depois de encerrado, o caso não aceita novas tentativas de contato nem ações; fica só para consulta.</span></div>
    ${campo({ rotulo: 'Data de encerramento', nome: 'data', tipo: 'date', obrigatorio: true, valor: hoje(), atributos: `min="${esc(d.abertura)}" max="${hoje()}"` })}
    ${campo({ rotulo: 'Desfecho', nome: 'desfecho', tipo: 'select', obrigatorio: true, vazio: 'Selecione', opcoes: DESFECHOS_CASO.map((x) => [x, x]) })}
    ${campo({ rotulo: 'Observação', nome: 'observacao', tipo: 'textarea', atributos: 'maxlength="500"' })}
    ${botoesModal('Encerrar caso', { tom: 'danger', largo: false })}
  </form>`),
});

registrarFormularios({
  'caso:abrir': async (dados) => {
    const caso = await abrirCaso(dados);
    fecharModal();
    navegar(`busca-ativa/${caso.id}`);
    toast('Caso aberto. Registre a primeira tentativa de contato.');
  },
  'caso:tentativa': async (dados) => { await registrarTentativa(dados); atualizar(); toast('Tentativa de contato registrada.'); },
  'caso:acao': async (dados) => { await registrarAcao(dados); atualizar(); toast('Ação registrada.'); },
  'caso:encerrar': async (dados) => { await encerrarCaso(dados); fecharModal(); atualizar(); toast('Caso encerrado. Ele fica disponível para consulta.'); },
});

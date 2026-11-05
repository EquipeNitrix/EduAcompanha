// Telas de turmas. Administrador: cadastra e altera. Secretaria: consulta.

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar, navegar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { ehAdmin, ehSecretaria } from '../../app/session.js';
import { TURNOS, turnosDaEscola } from '../../app/constantes.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { medidorFrequencia } from '../../components/frequency-meter.js';
import { filtroSelect } from '../../components/filters.js';
import { tabela } from '../../components/table.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { campo, botoesModal } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc, plural } from '../../utils/texto.js';
import { hoje, fmtData } from '../../utils/datas.js';
import { porNome } from '../../utils/listas.js';
import { listarTurmas, detalheTurma, salvarTurma } from './turmas.service.js';
import { nomeDoTurno } from '../configuracao/configuracao.service.js';
import { alunosSemMatriculaNaTurma, matricular } from '../alunos/alunos.service.js';

const SIGLA = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex'];

async function telaTurmas() {
  const f = estado('turmas', { ano: '', escola: '' });
  const { cfg, anoEscolhido, linhas } = await listarTurmas(f);
  const escolas = (await api.escolas.listar()).sort(porNome);
  const anos = Object.keys(cfg.anosLetivos).sort().reverse();
  return `${cabecalhoPagina({
    titulo: 'Turmas',
    subtitulo: '',
    acoes: ehAdmin() ? `<button class="btn btn-primary" type="button" data-acao="turma:nova">${icone('plus')}Nova turma</button>` : '',
  })}
  <section class="card">
    <div class="filters">
      ${filtroSelect({ rotulo: 'Ano letivo', chave: 'turmas.ano', valor: String(anoEscolhido), opcoes: anos.map((a) => [a, Number(a) === cfg.anoAtual ? `${a} (atual)` : a]), todas: null })}
      ${ehAdmin() ? filtroSelect({ rotulo: 'Escola', chave: 'turmas.escola', valor: f.escola, opcoes: escolas.map((e) => [e.id, e.nome]) }) : ''}
    </div>
    ${linhas.length ? tabela(
    ['Turma', 'Turno', ...(ehAdmin() ? ['Escola'] : []), { texto: 'Alunos ativos', classe: 'r' }, 'Aulas na semana', ''],
    linhas.map((l) => `<tr>
        <td><a class="row-link" href="#/turmas/${l.turma.id}">${esc(l.turma.nome)}</a><span class="sub">Ano letivo ${l.turma.ano}</span></td>
        <td>${TURNOS[l.turma.turno]}</td>
        ${ehAdmin() ? `<td class="small">${esc(l.escola?.nome || '')}</td>` : ''}
        <td class="r num">${l.alunosAtivos}</td>
        <td>${l.aulasSemana != null ? `<span class="num">${l.aulasSemana}</span>` : chip('Turno sem grade de horários', 'warn')}</td>
        <td><div class="row-actions">
          ${ehAdmin() && l.turma.ano === cfg.anoAtual ? `<button class="btn btn-ghost btn-sm" type="button" data-acao="turma:editar" data-id="${l.turma.id}">${icone('edit', 15)}Editar</button>` : ''}
          <a class="btn btn-ghost btn-sm" href="#/turmas/${l.turma.id}">Abrir</a>
        </div></td>
      </tr>`),
  ) : estadoVazio({
    icone: 'layers', titulo: 'Nenhuma turma neste ano letivo',
    texto: ehAdmin() ? 'Cadastre a turma informando nome, turno e escola.' : 'O administrador ainda não cadastrou turmas neste ano letivo.',
    acoes: ehAdmin() ? '<button class="btn btn-primary btn-sm" type="button" data-acao="turma:nova">Nova turma</button>' : '',
  })}
  </section>`;
}

function abaAlunos(d) {
  if (!d.alunos.length) {
    return estadoVazio({
      icone: 'users', titulo: 'Nenhum aluno nesta turma',
      texto: ehAdmin() ? 'Cadastre um aluno novo ou matricule um aluno que já está no sistema.' : 'O administrador ainda não matriculou alunos nesta turma.',
    });
  }
  return tabela(['Aluno', 'Início', 'Situação', 'Frequência'], d.alunos.map((x) => `<tr>
    <td><a class="row-link" href="#/alunos/${x.aluno.id}">${esc(x.aluno.nome)}</a><span class="sub mono">RA ${esc(x.aluno.ra)}</span></td>
    <td class="num">${fmtData(x.matricula.inicio)}</td>
    <td>${x.matricula.status === 'ativa' ? chip('Ativa', 'ok') : chip(`Encerrada${x.matricula.fim ? ` em ${fmtData(x.matricula.fim)}` : ''}`, 'muted', { semPonto: true })}</td>
    <td>${medidorFrequencia(x.freq)}</td>
  </tr>`));
}

function resumoDoTurno(d) {
  const turno = nomeDoTurno(d.turma.turno);
  if (!d.semana) {
    return `<div class="card-b" style="border-bottom:1px solid var(--line)"><div class="note crit">${icone('alert')}<span>A escola ainda não montou a grade de horários do ${turno}. Sem ela, não é possível lançar ocorrências nem calcular a frequência desta turma. <a href="#/turnos">Configurar turnos</a>.</span></div></div>`;
  }
  return `<div class="card-b" style="border-bottom:1px solid var(--line);display:flex;flex-wrap:wrap;gap:8px 16px;align-items:center">
    <span class="eyebrow">Aulas por dia no ${turno}</span>
    <span class="small">${SIGLA.map((dia, i) => `${dia} <b class="num">${d.semana[i]}</b>`).join(' · ')}</span>
    <a class="small" href="#/turnos">Ver turnos</a>
  </div>`;
}

async function telaTurma({ id }) {
  const d = await detalheTurma(id);
  const ativos = d.alunos.filter((x) => x.matricula.status === 'ativa').length;
  const acoes = !d.ativa ? chip('Ano encerrado · consulta', 'muted', { semPonto: true })
    : ehAdmin() ? `<button class="btn btn-ghost" type="button" data-acao="turma:editar" data-id="${id}">${icone('edit')}Editar</button>
      <button class="btn btn-ghost" type="button" data-acao="turma:matricular" data-id="${id}">Matricular aluno cadastrado</button>
      <button class="btn btn-primary" type="button" data-acao="aluno:novo" data-turma="${id}">${icone('plus')}Novo aluno</button>`
      : ehSecretaria() ? `<a class="btn btn-sun" href="#/frequencia/${id}">${icone('check')}Lançar frequência</a>` : '';
  return `${cabecalhoPagina({
    trilha: [['Turmas', 'turmas'], [d.turma.nome]],
    sobretitulo: `${esc(d.escola.nome)} · Ano letivo ${d.turma.ano}`,
    titulo: `${esc(d.turma.nome)} <span class="turno">${TURNOS[d.turma.turno]}</span>`,
    acoes,
  })}
  <section class="card">
    <div class="card-h"><h2>Alunos</h2>${chip(String(ativos), '', { semPonto: true })}</div>
    ${resumoDoTurno(d)}
    ${abaAlunos(d)}
  </section>`;
}

export const rotas = [
  { caminho: 'turmas', perfis: ['admin', 'secretaria'], tela: telaTurmas },
  { caminho: 'turmas/:id', perfis: ['admin', 'secretaria'], tela: telaTurma, menu: 'turmas' },
];

async function formularioTurma(id) {
  const turma = id ? await api.turmas.obter(id) : null;
  const escolas = (await api.escolas.listar()).sort(porNome);
  const cfg = await api.configuracao();
  abrirModal(turma ? 'Editar turma' : 'Nova turma', `<form data-form="turma:salvar" novalidate class="form-grid">
    <input type="hidden" name="id" value="${turma?.id || ''}">
    ${campo({ rotulo: 'Nome da turma', nome: 'nome', obrigatorio: true, valor: turma?.nome, atributos: 'maxlength="30" placeholder="Exemplo: 7º A"' })}
    ${campo({ rotulo: 'Turno', nome: 'turno', tipo: 'select', obrigatorio: true, valor: turma?.turno || '', vazio: 'Selecione', opcoes: (turma ? turnosDaEscola(escolas.find((e) => e.id === turma.escolaId)) : Object.keys(TURNOS)).map((t) => [t, TURNOS[t]]), dica: turma ? '' : 'Escolas integrais usam o turno Integral; as outras, manhã, tarde ou noite.' })}
    ${turma ? `<input type="hidden" name="escolaId" value="${turma.escolaId}">`
    : campo({ rotulo: 'Escola', nome: 'escolaId', tipo: 'select', obrigatorio: true, largo: true, vazio: 'Selecione', opcoes: escolas.map((e) => [e.id, e.nome]) })}
    <p class="small muted span-2">${turma ? `Ano letivo ${turma.ano}.` : `A turma será criada no ano letivo ${cfg.anoAtual}.`}</p>
    ${botoesModal(turma ? 'Salvar alterações' : 'Cadastrar turma')}
  </form>`);
}

registrarAcoes({
  'turma:nova': () => formularioTurma(null),
  'turma:editar': (d) => formularioTurma(d.id),
  'turma:matricular': async (d) => {
    const turma = await api.turmas.obter(d.id);
    const cfg = await api.configuracao();
    const anoLetivo = cfg.anosLetivos[turma.ano];
    const candidatos = await alunosSemMatriculaNaTurma(d.id);
    abrirModal(`Matricular em ${esc(turma.nome)}`, candidatos.length ? `<form data-form="turma:matricular" novalidate class="form-grid">
      <input type="hidden" name="turmaId" value="${turma.id}">
      ${campo({ rotulo: 'Aluno', nome: 'alunoId', tipo: 'select', obrigatorio: true, largo: true, vazio: 'Selecione', opcoes: candidatos.map((a) => [a.id, `${a.nome} · RA ${a.ra}`]), dica: 'Aparecem só alunos desta escola sem matrícula ativa no ano letivo.' })}
      ${campo({ rotulo: 'Início da matrícula', nome: 'inicio', tipo: 'date', obrigatorio: true, valor: hoje(), atributos: `min="${anoLetivo.inicio}" max="${anoLetivo.fim}"` })}
      ${botoesModal('Matricular')}
    </form>` : `${estadoVazio({ icone: 'users', titulo: 'Nenhum aluno disponível', texto: 'Todos os alunos desta escola já têm matrícula ativa no ano letivo. Cadastre um aluno novo.' })}
    <div class="modal-actions"><button class="btn btn-ghost" type="button" data-acao="modal:fechar">Fechar</button>
    <button class="btn btn-primary" type="button" data-acao="aluno:novo" data-turma="${turma.id}">Novo aluno</button></div>`);
  },
});

registrarFormularios({
  'turma:salvar': async (dados) => {
    const turma = await salvarTurma(dados);
    fecharModal();
    if (dados.id) { atualizar(); toast('Turma atualizada.'); return; }
    navegar(`turmas/${turma.id}`);
    toast('Turma cadastrada.');
  },
  'turma:matricular': async (dados) => { await matricular(dados); fecharModal(); atualizar(); toast('Aluno matriculado.'); },
});

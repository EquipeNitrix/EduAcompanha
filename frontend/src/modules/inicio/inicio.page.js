// Tela inicial: resumo do dia para a secretaria e visão da rede para o administrador.

import { usuarioAtual } from '../../app/session.js';
import { registrarAcoes } from '../../app/eventos.js';
import { navegar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { LIMITE_FREQUENCIA, TIPOS_OCORRENCIA } from '../../app/constantes.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { medidorFrequencia } from '../../components/frequency-meter.js';
import { tabela } from '../../components/table.js';
import { esc, iniciais, primeiroNome, plural } from '../../utils/texto.js';
import { hoje, dataPorExtenso } from '../../utils/datas.js';
import { indexarPor, porNome } from '../../utils/listas.js';
import { alunosEmAlerta, indicadores } from '../alerta/alerta.service.js';
import { cartaoDisparos } from '../notificacao/notificacao.page.js';
import { comAulas, horarioDoLancamento } from '../frequencia/frequencia.service.js';
import { mapaDeAulas } from '../configuracao/configuracao.service.js';

/** Hora do último movimento do registro: quando foi lançado ou, no "saiu e voltou", a volta. */
function ultimoMovimento(r) {
  const d = new Date(r.criadoEm);
  const lancado = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  return r.tipo === 'saidaRetorno' && r.horaEntrada > lancado ? r.horaEntrada : lancado;
}

function saudacao() {
  const h = new Date().getHours();
  return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite';
}

const kpi = (link, rotulo, valor, ic, tom, rodape) => `<a class="kpi tone-${tom}" href="#/${link}">
  <div class="top">${rotulo}<span class="badge-ico">${icone(ic)}</span></div>
  <div class="v">${valor}</div>${rodape ? `<div class="foot">${rodape}</div>` : ''}</a>`;

function cartaoAlertas(lista, maximo = 6) {
  return `<section class="card">
    <div class="card-h"><h2>Em alerta de evasão</h2><a href="#/alertas" class="small">Ver painel completo</a></div>
    ${lista.length ? `<div class="list">${lista.slice(0, maximo).map((x) => `<a class="list-item" href="#/alunos/${x.aluno.id}">
      <span class="avatar" style="background:var(--crit-soft);color:var(--crit-ink)">${iniciais(x.aluno.nome)}</span>
      <div class="grow"><div class="t">${esc(x.aluno.nome)}</div><div class="s">${esc(x.turma.nome)} · ${x.freq.aulasFalta} aulas com falta de ${x.freq.previstas}</div></div>
      ${medidorFrequencia(x.freq)}
    </a>`).join('')}</div>`
    : estadoVazio({ icone: 'ok', titulo: `Nenhum aluno abaixo de ${LIMITE_FREQUENCIA}%`, texto: 'Quando a frequência de alguém cair abaixo do mínimo, o nome aparece aqui automaticamente.' })}
  </section>`;
}

async function inicioSecretaria() {
  const usuario = usuarioAtual();
  const dia = hoje();
  const turmas = indexarPor(await api.turmas.listar({ escolaId: usuario.escolaId }));
  const aulas = await mapaDeAulas(usuario.escolaId);
  const semanaDaTurma = (id) => { const t = turmas.get(id); return t ? aulas.obter(t.escolaId, t.turno) : null; };
  const registros = comAulas(await api.lancamentos.listar({ escolaId: usuario.escolaId, data: dia, status: 'ativo' }), semanaDaTurma);
  const alunos = indexarPor(await api.alunos.listar({ id: registros.map((r) => r.alunoId) }));
  const alertas = await alunosEmAlerta();
  const aguardando = (await api.notificacoes.listar({ escolaId: usuario.escolaId, status: 'aguardando' })).length;
  const casos = (await api.casos.listar({ escolaId: usuario.escolaId, status: 'aberto' })).length;
  const faltas = registros.filter((r) => r.tipo === 'falta');

  return `${cabecalhoPagina({
    sobretitulo: dataPorExtenso(),
    titulo: `${saudacao()}, ${esc(primeiroNome(usuario.nome))}`,
    subtitulo: 'Painel Inicial: Frequência de hoje e acompanhamento da escola.',
    acoes: `<a class="btn btn-primary" href="#/frequencia">${icone('check')}Lançar frequência</a>`,
  })}
  <div class="kpis">
    ${kpi('frequencia', 'Faltas hoje', faltas.length, 'check', 'info', plural(registros.length, 'registro no dia', 'registros no dia'))}
    ${kpi('alertas', 'Em alerta de evasão', alertas.length, 'alert', alertas.length ? 'crit' : 'ok', `Abaixo de ${LIMITE_FREQUENCIA}% no ano`)}
    ${kpi('notificacoes', 'Aguardando resposta', aguardando, 'msg', 'warn', 'Mensagens aos responsáveis')}
    ${kpi('busca-ativa', 'Casos de Busca Ativa', casos, 'compass', 'info', 'Em acompanhamento')}
  </div>
  <div class="grid-3-1">
    <section class="card">
      <div class="card-h"><h2>Registros de hoje</h2><a href="#/frequencia" class="small">Abrir turmas</a></div>
      ${registros.length ? `<div class="list">${registros.map((r) => ({ ...r, ultimo: ultimoMovimento(r) })).sort((a, b) => b.ultimo.localeCompare(a.ultimo)).map((r) => `
        <div class="list-item registro-hoje">
          <span class="hora num">${r.ultimo}</span>
          <div class="grow" style="min-width:0"><a class="t row-link" href="#/alunos/${r.alunoId}">${esc(alunos.get(r.alunoId)?.nome)}</a><div class="s">${esc(turmas.get(r.turmaId)?.nome || '')}${r.tipo !== 'falta' ? ` · ${horarioDoLancamento(r)}` : ''}${r.qtdAulas != null ? ` · ${plural(r.qtdAulas, 'aula perdida', 'aulas perdidas')}` : ''}</div></div>
          ${chip(TIPOS_OCORRENCIA[r.tipo], r.tipo === 'falta' ? 'crit' : r.tipo === 'atraso' ? 'warn' : 'info')}
          <button class="btn btn-ghost btn-sm" type="button" data-acao="inicio:irTurma" data-turma="${r.turmaId}" title="Abre o lançamento de frequência desta turma, na data de hoje">Ir para a turma${icone('arrowRight', 15)}</button>
        </div>`).join('')}</div>`
    : estadoVazio({ icone: 'check', titulo: 'Nenhum registro hoje', texto: 'Na chamada do dia, registre só quem faltou, chegou atrasado ou saiu mais cedo.', acoes: '<a class="btn btn-primary btn-sm" href="#/frequencia">Lançar frequência</a>' })}
    </section>
    ${await cartaoDisparos([usuario.escolaId], false)}
  </div>
  ${cartaoAlertas(alertas)}`;
}

async function inicioAdmin() {
  const cfg = await api.configuracao();
  const escolas = (await api.escolas.listar()).sort(porNome);
  const usuarios = await api.usuarios.listar({ perfil: 'secretaria', ativo: true });
  const todos = await indicadores();
  const alertas = todos.filter((x) => x.freq.emAlerta).sort((a, b) => a.freq.pct - b.freq.pct);
  const faltasHoje = await api.lancamentos.listar({ data: hoje(), tipo: 'falta', status: 'ativo' });

  return `${cabecalhoPagina({
    sobretitulo: 'Nitrix',
    titulo: `${saudacao()}, administrador`,
    subtitulo: 'Visão de todas as escolas atendidas pelo EduAcompanha.',
    acoes: `<a class="btn btn-ghost" href="#/usuarios">${icone('usercog')}Usuários</a><a class="btn btn-primary" href="#/escolas">${icone('school')}Escolas</a>`,
  })}
  <div class="kpis">
    ${kpi('escolas', 'Escolas', escolas.length, 'school', 'info', 'Cadastradas no sistema')}
    ${kpi('usuarios', 'Usuários da secretaria', usuarios.length, 'usercog', 'info', 'Com acesso ativo')}
    ${kpi('alunos', 'Alunos matriculados', todos.length, 'users', 'ok', `Ano letivo ${cfg.anoAtual}`)}
    ${kpi('alertas', 'Em alerta de evasão', alertas.length, 'alert', alertas.length ? 'crit' : 'ok', 'Todas as escolas')}
  </div>
  <section class="card">
    <div class="card-h"><h2>Escolas</h2><a href="#/escolas" class="small">Gerenciar</a></div>
    ${tabela(
    ['Escola', 'CIE', { texto: 'Turmas', classe: 'r' }, { texto: 'Alunos', classe: 'r' }, { texto: 'Em alerta', classe: 'r' }, { texto: 'Faltas hoje', classe: 'r' }],
    escolas.map((e) => {
      const daEscola = todos.filter((x) => x.turma.escolaId === e.id);
      return `<tr>
          <td><b>${esc(e.nome)}</b><span class="sub">${esc(e.municipio || '')}</span></td>
          <td class="mono">${esc(e.cie)}</td>
          <td class="r num">${new Set(daEscola.map((x) => x.turma.id)).size}</td>
          <td class="r num">${daEscola.length}</td>
          <td class="r num">${daEscola.filter((x) => x.freq.emAlerta).length}</td>
          <td class="r num">${faltasHoje.filter((f) => f.escolaId === e.id).length}</td>
        </tr>`;
    }),
  )}
  </section>
  <div class="grid-2">${cartaoAlertas(alertas, 5)}${await cartaoDisparos(escolas.map((e) => e.id), true)}</div>`;
}

export const rotas = [
  { caminho: 'inicio', perfis: ['admin', 'secretaria'], tela: () => (usuarioAtual().perfil === 'admin' ? inicioAdmin() : inicioSecretaria()) },
];

registrarAcoes({
  'inicio:irTurma': (d) => { Object.assign(estado(`freq:${d.turma}`, { q: '' }), { data: hoje() }); navegar(`frequencia/${d.turma}`); },
});

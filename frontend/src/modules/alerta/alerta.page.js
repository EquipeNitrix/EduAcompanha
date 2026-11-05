// Painel de alerta de evasão.

import { registrarAcoes } from '../../app/eventos.js';
import { estado } from '../../app/estado.js';
import { ehAdmin, ehSecretaria, usuarioAtual } from '../../app/session.js';
import { LIMITE_FREQUENCIA, LIMITE_ATENCAO, TURNOS } from '../../app/constantes.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { medidorFrequencia } from '../../components/frequency-meter.js';
import { filtroSelect } from '../../components/filters.js';
import { tabela } from '../../components/table.js';
import { abrirExportacao } from '../../components/export.js';
import { esc, plural } from '../../utils/texto.js';
import { fmtPct } from '../../utils/formatos.js';
import { porNome, indexarPor } from '../../utils/listas.js';
import { alunosEmAlerta } from './alerta.service.js';

async function opcoes(f) {
  const cfg = await api.configuracao();
  const escolas = (await api.escolas.listar()).sort(porNome);
  const escolaId = ehAdmin() ? f.escola || undefined : usuarioAtual().escolaId;
  const turmas = (await api.turmas.listar({ ano: cfg.anoAtual, escolaId })).sort(porNome);
  return { escolas, turmas };
}

async function telaAlertas() {
  const f = estado('alertas', { escola: '', turma: '' });
  const [lista, op] = await Promise.all([alunosEmAlerta(f), opcoes(f)]);
  const casosPorAluno = indexarPor(await api.casos.listar({ status: 'aberto' }), 'alunoId');
  const escolas = indexarPor(op.escolas);

  return `${cabecalhoPagina({
    titulo: 'Alerta de evasão',
    subtitulo: `Alunos com frequência no ano letivo abaixo de ${LIMITE_ATENCAO}% entram na lista automaticamente: em atenção entre ${LIMITE_FREQUENCIA}% e ${LIMITE_ATENCAO}%, e críticos abaixo de ${LIMITE_FREQUENCIA}%.`,
    acoes: `<button class="btn btn-ghost" type="button" data-acao="alerta:exportar" ${lista.length ? '' : 'disabled'}>${icone('download')}Exportar</button>`,
  })}
  <section class="card">
    <div class="filters">
      ${ehAdmin() ? filtroSelect({ rotulo: 'Escola', chave: 'alertas.escola', valor: f.escola, opcoes: op.escolas.map((e) => [e.id, e.nome]), limpa: 'alertas.turma' }) : ''}
      ${filtroSelect({ rotulo: 'Turma', chave: 'alertas.turma', valor: f.turma, opcoes: op.turmas.map((t) => [t.id, `${t.nome} · ${TURNOS[t.turno]}`]) })}
      <div class="filter" style="max-width:none;align-self:center"><span class="small muted">Do menor para o maior percentual · ${plural(lista.length, 'aluno', 'alunos')}</span></div>
    </div>
    ${lista.length ? tabela(
    ['Aluno', 'Turma', ...(ehAdmin() ? ['Escola'] : []), 'Frequência', { texto: 'Aulas com falta', classe: 'r' }, { texto: 'Aulas previstas', classe: 'r' }, 'Situação', 'Busca Ativa'],
    lista.map((x) => {
      const caso = casosPorAluno.get(x.aluno.id);
      const situacao = x.nivel === 'critico' ? chip('Crítico', 'crit') : chip('Atenção', 'warn');
      const busca = caso
        ? (ehSecretaria() ? `<a href="#/busca-ativa/${caso.id}">${chip('Caso aberto', 'sun')}</a>` : chip('Caso aberto', 'sun'))
        : '<span class="muted small">Sem caso</span>';
      return `<tr>
        <td><a class="row-link" href="#/alunos/${x.aluno.id}">${esc(x.aluno.nome)}</a><span class="sub mono">RA ${esc(x.aluno.ra)}</span></td>
        <td>${esc(x.turma.nome)}<span class="sub">${TURNOS[x.turma.turno]}</span></td>
        ${ehAdmin() ? `<td class="small">${esc(escolas.get(x.turma.escolaId)?.nome || '')}</td>` : ''}
        <td>${medidorFrequencia(x.freq)}</td>
        <td class="r num">${x.freq.aulasFalta}</td>
        <td class="r num">${x.freq.previstas}</td>
        <td>${situacao}</td>
        <td>${busca}</td>
      </tr>`;
    }),
  ) : estadoVazio({
    icone: 'ok', titulo: 'Nenhum aluno em alerta',
    texto: f.turma || f.escola ? `Nenhum aluno abaixo de ${LIMITE_ATENCAO}% com estes filtros.` : `Todos os alunos estão com frequência igual ou acima de ${LIMITE_ATENCAO}% no ano letivo.`,
  })}
  </section>`;
}

export const rotas = [
  { caminho: 'alertas', perfis: ['admin', 'secretaria'], tela: telaAlertas },
];

registrarAcoes({
  'alerta:exportar': async () => {
    const f = estado('alertas');
    const [lista, op] = await Promise.all([alunosEmAlerta(f), opcoes(f)]);
    const escolas = indexarPor(op.escolas);
    const turma = op.turmas.find((t) => t.id === f.turma);
    const escolaNome = ehAdmin() ? (f.escola ? escolas.get(f.escola)?.nome : 'todas') : escolas.get(usuarioAtual().escolaId)?.nome;
    abrirExportacao({
      titulo: 'Alerta de evasão',
      filtros: `Escola: ${escolaNome} · Turma: ${turma ? turma.nome : 'todas'} · Frequência abaixo de ${LIMITE_ATENCAO}%`,
      colunas: ['Aluno', 'RA', 'Turma', 'Escola', 'Frequência', 'Aulas com falta', 'Aulas previstas', 'Situação'],
      linhas: lista.map((x) => [x.aluno.nome, x.aluno.ra, x.turma.nome, escolas.get(x.turma.escolaId)?.nome || '', fmtPct(x.freq.pct), x.freq.aulasFalta, x.freq.previstas, x.nivel === 'critico' ? 'Crítico' : 'Atenção']),
      nomeArquivo: 'alerta-de-evasao',
    });
  },
});

// Cadastro de escolas (administrador).

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar, navegar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { tabela } from '../../components/table.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { campo, botoesModal } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc, plural } from '../../utils/texto.js';
import { chip } from '../../components/chip.js';
import { listarEscolas, salvarEscola } from './admin.service.js';
import { TURNOS, MODELOS_TURNO, turnosDoModelo } from '../../app/constantes.js';

async function telaEscolas() {
  const linhas = await listarEscolas();
  return `${cabecalhoPagina({
    titulo: 'Escolas',
    subtitulo: 'Código CIE não pode repetir.',
    acoes: `<button class="btn btn-primary" type="button" data-acao="escola:nova">${icone('plus')}Nova escola</button>`,
  })}
  <section class="card">
    ${linhas.length ? tabela(
    ['CIE', 'Escola', 'Município', 'Modelo de turno', { texto: 'Usuários', classe: 'r' }, { texto: 'Turmas no ano', classe: 'r' }, 'Turnos', ''],
    linhas.map(({ escola, usuarios, turmas, turnosPendentes, modelo }) => `<tr>
        <td class="mono">${esc(escola.cie)}</td>
        <td><b>${esc(escola.nome)}</b></td>
        <td>${esc(escola.municipio || '-')}</td>
        <td class="small">${esc(modelo)}</td>
        <td class="r num">${usuarios}</td>
        <td class="r num">${turmas}</td>
        <td>${turnosPendentes ? chip(plural(turnosPendentes, 'turno a configurar', 'turnos a configurar'), 'warn') : chip('Em dia', 'ok')}</td>
        <td><div class="row-actions">
          <button class="btn btn-ghost btn-sm" type="button" data-acao="escola:turnos" data-id="${escola.id}">${icone('clock', 15)}Turnos e envio</button>
          <button class="btn btn-ghost btn-sm" type="button" data-acao="escola:editar" data-id="${escola.id}">${icone('edit', 15)}Editar</button>
        </div></td>
      </tr>`),
  ) : estadoVazio({ icone: 'school', titulo: 'Nenhuma escola cadastrada', texto: 'Cadastre a escola antes de criar o acesso da secretaria e as turmas.', acoes: '<button class="btn btn-primary btn-sm" type="button" data-acao="escola:nova">Nova escola</button>' })}
  </section>`;
}

async function formulario(id) {
  const escola = id ? await api.escolas.obter(id) : null;
  const modelo = escola?.modeloTurno || 'parcial';
  const blocoEnvio = (m) => `<div class="turno-form-campos envio-${m}" data-grupo="modeloTurno" data-grupo-valor="${m}" ${m === modelo ? '' : 'hidden'}>
      ${turnosDoModelo(m).map((t) => campo({ prefixo: 'esc', rotulo: TURNOS[t], nome: `envio_${t}`, tipo: 'time', valor: escola?.horarioEnvio?.[t] || '' })).join('')}
    </div>`;
  abrirModal(escola ? 'Editar escola' : 'Nova escola', `<form data-form="escola:salvar" novalidate class="form-grid">
    <input type="hidden" name="id" value="${escola?.id || ''}">
    ${campo({ rotulo: 'Código CIE', nome: 'cie', obrigatorio: true, valor: escola?.cie, atributos: 'inputmode="numeric" maxlength="6" placeholder="035262"', dica: 'Código da escola na Secretaria da Educação: 6 números.' })}
    ${campo({ rotulo: 'Município', nome: 'municipio', valor: escola?.municipio, atributos: 'maxlength="60"' })}
    ${campo({ rotulo: 'Nome da escola', nome: 'nome', obrigatorio: true, largo: true, valor: escola?.nome, atributos: 'maxlength="150" placeholder="E.E. ..."' })}
    ${campo({ rotulo: 'Modelo de turno', nome: 'modeloTurno', tipo: 'select', obrigatorio: true, largo: true, valor: modelo, opcoes: Object.entries(MODELOS_TURNO), atributos: 'data-mostra-grupo="modeloTurno"', dica: 'Integral: um turno só, o dia todo (como nas escolas PEI). Manhã, tarde e noite: cada turma estuda em um desses turnos.' })}
    <fieldset class="fieldset span-2"><legend>Horário de envio das mensagens de falta (opcional)</legend>
      ${blocoEnvio('parcial')}
      ${blocoEnvio('integral')}
      <p class="hint" style="margin:0">Pode deixar em branco: a secretaria define depois. A grade de horários das aulas de cada turno é montada em <b>Turnos e envio</b>.</p>
    </fieldset>
    ${botoesModal(escola ? 'Salvar alterações' : 'Cadastrar escola')}
  </form>`, { largo: true });
}

export const rotas = [{ caminho: 'escolas', perfis: ['admin'], tela: telaEscolas }];

registrarAcoes({
  'escola:nova': () => formulario(null),
  'escola:editar': (d) => formulario(d.id),
  'escola:turnos': (d) => { estado('turnos').escola = d.id; navegar('turnos'); },
});

registrarFormularios({
  'escola:salvar': async (dados) => {
    await salvarEscola(dados);
    fecharModal();
    atualizar();
    toast(dados.id ? 'Escola atualizada.' : 'Escola cadastrada. Monte a grade de horários em "Turnos e envio".');
  },
});

// Usuários da secretaria (administrador).

import { registrarAcoes, registrarFormularios } from '../../app/eventos.js';
import { atualizar } from '../../app/router.js';
import { estado } from '../../app/estado.js';
import { api } from '../../data/api.js';
import { cabecalhoPagina } from '../../components/page-header.js';
import { chip } from '../../components/chip.js';
import { icone } from '../../components/icons.js';
import { estadoVazio } from '../../components/empty-state.js';
import { filtroBusca } from '../../components/filters.js';
import { tabela } from '../../components/table.js';
import { abrirModal, fecharModal } from '../../components/modal.js';
import { campo, botoesModal, botaoCarregando } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { esc } from '../../utils/texto.js';
import { fmtHora } from '../../utils/datas.js';
import { porNome } from '../../utils/listas.js';
import { listarUsuarios, criarUsuario, editarUsuario, definirNovaSenha, desbloquearUsuario } from './admin.service.js';

const bloqueado = (u) => u.bloqueadoAte && Date.now() < u.bloqueadoAte;

function situacao(u) {
  if (!u.ativo) return chip('Desativado', 'muted', { semPonto: true });
  if (bloqueado(u)) return chip(`Bloqueado até ${fmtHora(u.bloqueadoAte)}`, 'crit');
  return chip('Ativo', 'ok');
}

async function telaUsuarios() {
  const f = estado('usuarios', { q: '' });
  const linhas = await listarUsuarios(f);
  return `${cabecalhoPagina({
    titulo: 'Usuários',
    subtitulo: 'Cada usuário da secretaria pertence a uma única escola e só vê os dados dela. O login não pode repetir no sistema.',
    acoes: `<button class="btn btn-primary" type="button" data-acao="usuario:novo">${icone('plus')}Novo usuário da secretaria</button>`,
  })}
  <section class="card">
    <div class="filters">${filtroBusca({ chave: 'usuarios.q', valor: f.q, placeholder: 'Nome ou login' })}</div>
    ${linhas.length ? tabela(
    ['Usuário', 'Login', 'Perfil', 'Escola', 'Situação', ''],
    linhas.map(({ usuario: u, escola }) => `<tr>
        <td><b>${esc(u.nome)}</b></td>
        <td class="mono">${esc(u.login)}</td>
        <td>${u.perfil === 'admin' ? chip('Administrador', 'info', { semPonto: true }) : chip('Secretaria', '', { semPonto: true })}</td>
        <td class="small">${escola ? esc(escola.nome) : '<span class="muted">Todas (Equipe Nitrix)</span>'}</td>
        <td>${situacao(u)}</td>
        <td><div class="row-actions">${u.perfil === 'secretaria' ? `
          ${bloqueado(u) ? `<button class="btn btn-ghost btn-sm" type="button" data-acao="usuario:desbloquear" data-id="${u.id}">${icone('unlock', 15)}Desbloquear</button>` : ''}
          <button class="btn btn-ghost btn-sm" type="button" data-acao="usuario:senha" data-id="${u.id}">${icone('lock', 15)}Nova senha</button>
          <button class="btn btn-ghost btn-sm" type="button" data-acao="usuario:editar" data-id="${u.id}">${icone('edit', 15)}Editar</button>`
    : '<span class="small muted">Conta da equipe</span>'}</div></td>
      </tr>`),
  ) : estadoVazio({ icone: 'search', titulo: 'Nenhum usuário encontrado', texto: 'Ajuste a busca.' })}
  </section>`;
}

async function opcoesEscolas() {
  return (await api.escolas.listar()).sort(porNome).map((e) => [e.id, e.nome]);
}

export const rotas = [{ caminho: 'usuarios', perfis: ['admin'], tela: telaUsuarios }];

registrarAcoes({
  'usuario:novo': async () => {
    const escolas = await opcoesEscolas();
    if (!escolas.length) { toast('Cadastre uma escola antes de criar usuários.', 'crit'); return; }
    abrirModal('Novo usuário da secretaria', `<form data-form="usuario:novo" novalidate class="form-grid">
      ${campo({ rotulo: 'Nome', nome: 'nome', obrigatorio: true, atributos: 'maxlength="120"' })}
      ${campo({ rotulo: 'Login', nome: 'login', obrigatorio: true, atributos: 'maxlength="40" autocapitalize="none" spellcheck="false" placeholder="nome.sobrenome"', dica: 'Letras minúsculas, números e ponto.' })}
      ${campo({ rotulo: 'Escola', nome: 'escolaId', tipo: 'select', obrigatorio: true, largo: true, vazio: 'Selecione', opcoes: escolas, dica: 'O usuário acessa somente os dados desta escola.' })}
      ${campo({ rotulo: 'Senha inicial', nome: 'senha', tipo: 'password', obrigatorio: true, atributos: 'autocomplete="new-password"', dica: 'Mínimo de 8 caracteres, com letras e números.' })}
      ${campo({ rotulo: 'Confirme a senha', nome: 'senha2', tipo: 'password', obrigatorio: true, atributos: 'autocomplete="new-password"' })}
      ${botoesModal('Criar usuário')}
    </form>`);
  },
  'usuario:editar': async (d) => {
    const u = await api.usuarios.obter(d.id);
    abrirModal('Editar usuário', `<form data-form="usuario:editar" novalidate class="form-grid">
      <input type="hidden" name="id" value="${u.id}">
      ${campo({ rotulo: 'Nome', nome: 'nome', obrigatorio: true, valor: u.nome, atributos: 'maxlength="120"' })}
      ${campo({ rotulo: 'Login', nome: 'login', valor: u.login, atributos: 'readonly', dica: 'O login não pode ser alterado.' })}
      ${campo({ rotulo: 'Escola', nome: 'escolaId', tipo: 'select', obrigatorio: true, largo: true, valor: u.escolaId, opcoes: await opcoesEscolas() })}
      ${campo({ rotulo: 'Acesso', nome: 'ativo', tipo: 'select', obrigatorio: true, valor: u.ativo ? '1' : '0', opcoes: [['1', 'Ativo'], ['0', 'Desativado']] })}
      ${botoesModal('Salvar alterações')}
    </form>`);
  },
  'usuario:senha': async (d) => {
    const u = await api.usuarios.obter(d.id);
    abrirModal('Definir nova senha', `<form data-form="usuario:senha" novalidate class="form-grid one">
      <input type="hidden" name="id" value="${u.id}">
      <p>Nova senha para <b>${esc(u.nome)}</b> (<span class="mono">${esc(u.login)}</span>). Passe a senha pessoalmente ao usuário; ela não aparece em nenhuma tela depois de salva.</p>
      ${campo({ rotulo: 'Nova senha', nome: 'senha', tipo: 'password', obrigatorio: true, atributos: 'autocomplete="new-password"', dica: 'Mínimo de 8 caracteres, com letras e números.' })}
      ${campo({ rotulo: 'Confirme a senha', nome: 'senha2', tipo: 'password', obrigatorio: true, atributos: 'autocomplete="new-password"' })}
      ${botoesModal('Salvar nova senha', { largo: false })}
    </form>`);
  },
  'usuario:desbloquear': async (d, botao) => {
    botaoCarregando(botao, true);
    const u = await desbloquearUsuario(d.id);
    atualizar();
    toast(`Acesso de ${u.nome} desbloqueado.`);
  },
});

registrarFormularios({
  'usuario:novo': async (dados) => { await criarUsuario(dados); fecharModal(); atualizar(); toast('Usuário criado. Informe o login e a senha a ele pessoalmente.'); },
  'usuario:editar': async (dados) => { await editarUsuario(dados.id, dados); fecharModal(); atualizar(); toast('Usuário atualizado.'); },
  'usuario:senha': async (dados) => { await definirNovaSenha(dados.id, dados); fecharModal(); atualizar(); toast('Nova senha salva.'); },
});

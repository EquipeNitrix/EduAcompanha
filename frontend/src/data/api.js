// =====================================================================
// PONTO ÚNICO DE ACESSO AOS DADOS
// ---------------------------------------------------------------------
// Os services dos módulos falam só com este arquivo. Hoje ele usa o mock
// salvo no navegador (data/mock). Quando o back-end existir, troque o corpo
// destas funções por chamadas fetch à API. As telas não mudam.
//
// Exemplo de como ficaria com o back-end:
//   listar: (filtros) => fetch(`/api/alunos?${new URLSearchParams(filtros)}`).then(r => r.json())
// =====================================================================

import * as store from './mock/store.js';
import { hashSenha } from './mock/senha.js';
import { RegraNegocio } from '../utils/validacao.js';

const MAX_TENTATIVAS_LOGIN = 5;
const BLOQUEIO_MINUTOS = 15;

/** Simula o tempo de resposta de um servidor nas operações de escrita. */
const espera = (min = 80, max = 180) => new Promise((ok) => setTimeout(ok, min + Math.random() * (max - min)));

const semSenha = (usuario) => {
  if (!usuario) return usuario;
  const { senhaHash, ...resto } = usuario;
  return resto;
};

/** Cria as operações padrão de uma coleção (equivalente a um endpoint REST). */
function recurso(nome, prefixo, { saida = (x) => x } = {}) {
  return {
    listar: async (filtros = {}) => store.listar(nome, filtros).map(saida),
    obter: async (id) => saida(store.obter(nome, id)),
    criar: async (dados) => { await espera(); return saida(store.criar(nome, dados, prefixo)); },
    atualizar: async (id, alteracoes) => { await espera(); return saida(store.atualizar(nome, id, alteracoes)); },
    remover: async (id) => { await espera(); store.remover(nome, id); },
  };
}

export const api = {
  /** Abre o banco de demonstração (cria os dados de exemplo na primeira vez). */
  iniciar() { store.carregar(); },

  async restaurarExemplo() { await espera(200, 400); store.restaurar(); },

  /** Ano letivo atual e datas de início e fim de cada ano. */
  async configuracao() { return store.configuracao(); },

  /** Login. No back-end real: POST /auth/login. */
  async autenticar(login, senha) {
    await espera(250, 450);
    const usuario = store.encontrar('usuarios', (u) => u.login === login);
    const agora = Date.now();
    if (usuario?.bloqueadoAte && agora < usuario.bloqueadoAte) {
      const minutos = Math.ceil((usuario.bloqueadoAte - agora) / 60000);
      throw new RegraNegocio(`Acesso bloqueado após ${MAX_TENTATIVAS_LOGIN} tentativas incorretas. Tente de novo em ${minutos} min ou peça o desbloqueio ao administrador.`);
    }
    if (!usuario || !usuario.ativo || usuario.senhaHash !== hashSenha(senha)) {
      if (usuario) {
        const falhas = (usuario.falhas || 0) + 1;
        if (falhas >= MAX_TENTATIVAS_LOGIN) {
          store.atualizar('usuarios', usuario.id, { falhas: 0, bloqueadoAte: agora + BLOQUEIO_MINUTOS * 60000 });
          throw new RegraNegocio(`Acesso bloqueado por ${BLOQUEIO_MINUTOS} minutos após ${MAX_TENTATIVAS_LOGIN} tentativas incorretas.`);
        }
        store.atualizar('usuarios', usuario.id, { falhas });
      }
      throw new RegraNegocio('Login ou senha incorretos.');
    }
    return semSenha(store.atualizar('usuarios', usuario.id, { falhas: 0, bloqueadoAte: null }));
  },

  /** Define a senha de um usuário. No back-end real: o servidor gera o hash. */
  async definirSenha(usuarioId, senha) {
    await espera();
    store.atualizar('usuarios', usuarioId, { senhaHash: hashSenha(senha), falhas: 0, bloqueadoAte: null });
  },

  escolas: recurso('escolas', 'esc'),
  usuarios: recurso('usuarios', 'usr', { saida: semSenha }),
  horarios: recurso('horarios', 'hor'),
  turmas: recurso('turmas', 'tur'),
  alunos: recurso('alunos', 'alu'),
  matriculas: recurso('matriculas', 'mat'),
  responsaveis: recurso('responsaveis', 'rsp'),
  telefones: recurso('telefones', 'tel'),
  lancamentos: recurso('lancamentos', 'lan'),
  notificacoes: recurso('notificacoes', 'ntf'),
  casos: recurso('casos', 'cas'),
  acompanhamentos: recurso('acompanhamentos', 'acp'),
};

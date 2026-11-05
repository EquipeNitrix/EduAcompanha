// Usuário logado e checagens de permissão por perfil.
// Perfis: "admin" (Equipe Nitrix) e "secretaria" (uma escola).

import { AcessoNegado } from '../utils/validacao.js';

const CHAVE = 'eduacompanha.sessao';
let usuario = null;

export function definirUsuario(novo) {
  usuario = novo;
  try {
    if (novo) sessionStorage.setItem(CHAVE, novo.id);
    else sessionStorage.removeItem(CHAVE);
  } catch { /* sem armazenamento: sessão só em memória */ }
}

export function idDaSessaoSalva() {
  try { return sessionStorage.getItem(CHAVE); } catch { return null; }
}

export const usuarioAtual = () => usuario;
export const ehAdmin = () => usuario?.perfil === 'admin';
export const ehSecretaria = () => usuario?.perfil === 'secretaria';

/** Admin vê todas as escolas; a secretaria só a dela. */
export const podeVerEscola = (escolaId) => ehAdmin() || (usuario && usuario.escolaId === escolaId);

/** Escola usada nos filtros: undefined = todas (admin). */
export const escolaDoUsuario = () => (ehAdmin() ? undefined : usuario?.escolaId);

export function exigirPerfil(...perfis) {
  if (!usuario || !perfis.includes(usuario.perfil)) throw new AcessoNegado();
}

export function exigirEscola(escolaId) {
  if (!podeVerEscola(escolaId)) throw new AcessoNegado('Você não tem acesso aos dados desta escola.');
}

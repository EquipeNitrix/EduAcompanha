// Login, logout e retomada da sessão ao recarregar a página.

import { api } from '../../data/api.js';
import { definirUsuario, idDaSessaoSalva } from '../../app/session.js';
import { normalizar } from '../../utils/texto.js';
import { validar } from '../../utils/validacao.js';

export async function entrar(login, senha) {
  validar({ _: !String(login || '').trim() || !senha ? 'Informe o login e a senha.' : '' });
  const usuario = await api.autenticar(normalizar(login), senha);
  definirUsuario(usuario);
  return usuario;
}

export function sair() {
  definirUsuario(null);
}

export async function restaurarSessao() {
  const id = idDaSessaoSalva();
  if (!id) { definirUsuario(null); return null; }
  const usuario = await api.usuarios.obter(id);
  definirUsuario(usuario && usuario.ativo ? usuario : null);
  return usuario;
}

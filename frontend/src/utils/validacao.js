// Regras de validação e tipos de erro.
// Os services lançam estes erros; o app/eventos.js decide como mostrar cada um.

export const TELEFONE_RE = /^\(\d{2}\) 9?\d{4}-\d{4}$/;
export const RA_RE = /^\d{6,12}-?[\dXx]?$/;
export const LOGIN_RE = /^[a-z0-9.]{4,40}$/;
export const CIE_RE = /^\d{6}$/;

export const senhaValida = (s) => s.length >= 8 && /[a-zA-Z]/.test(s) && /\d/.test(s);

/** Erro em campos de formulário. `campos` = { nomeDoCampo: 'mensagem' }. A chave "_" é um aviso geral. */
export class ErroValidacao extends Error {
  constructor(campos) {
    super('Há campos para corrigir.');
    this.campos = campos;
  }
}

/** Regra de negócio violada. A mensagem é mostrada como está para o usuário. */
export class RegraNegocio extends Error {}

export class AcessoNegado extends Error {
  constructor(mensagem = 'Seu perfil não tem permissão para esta ação.') {
    super(mensagem);
  }
}

export class NaoEncontrado extends Error {
  constructor(mensagem = 'O registro não foi encontrado. Ele pode ter sido removido.') {
    super(mensagem);
  }
}

/**
 * Recebe { campo: mensagemOuVazio } e lança ErroValidacao com os campos que têm mensagem.
 * Exemplo: validar({ nome: !nome ? 'Informe o nome.' : '' })
 */
export function validar(regras) {
  const erros = Object.fromEntries(Object.entries(regras).filter(([, msg]) => msg));
  if (Object.keys(erros).length) throw new ErroValidacao(erros);
}

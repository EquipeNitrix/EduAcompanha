// Hash de senha SÓ PARA A DEMONSTRAÇÃO.
// No back-end real, use bcrypt ou argon2 no servidor. A senha nunca deve ser
// guardada nem comparada no navegador.

export function hashSenha(senha) {
  let h = 2166136261;
  for (const c of `edu:${senha}`) {
    h ^= c.charCodeAt(0);
    h = Math.imul(h, 16777619);
  }
  return `demo$${(h >>> 0).toString(16)}`;
}

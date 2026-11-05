// Administração da rede (somente Equipe Nitrix): escolas e usuários.

import { api } from '../../data/api.js';
import { exigirPerfil } from '../../app/session.js';
import { porNome, indexarPor } from '../../utils/listas.js';
import { normalizar, limparEspacos } from '../../utils/texto.js';
import { validar, RegraNegocio, NaoEncontrado, CIE_RE, LOGIN_RE, senhaValida } from '../../utils/validacao.js';
import { validarHorario, mapaDeAulas } from '../configuracao/configuracao.service.js';
import { TURNOS, MODELOS_TURNO, turnosDaEscola, turnosDoModelo } from '../../app/constantes.js';

// ---------- Escolas ----------

export async function listarEscolas() {
  exigirPerfil('admin');
  const cfg = await api.configuracao();
  const escolas = (await api.escolas.listar()).sort(porNome);
  const usuarios = await api.usuarios.listar();
  const turmas = await api.turmas.listar({ ano: cfg.anoAtual });
  const aulas = await mapaDeAulas();
  return escolas.map((escola) => {
    const daEscola = turmas.filter((t) => t.escolaId === escola.id);
    const turnosUsados = [...new Set(daEscola.map((t) => t.turno))];
    return {
      escola,
      usuarios: usuarios.filter((u) => u.escolaId === escola.id).length,
      turmas: daEscola.length,
      turnosPendentes: turnosUsados.filter((t) => !aulas.obter(escola.id, t) || !escola.horarioEnvio?.[t]).length,
      modelo: MODELOS_TURNO[escola.modeloTurno || 'parcial'],
    };
  });
}

/**
 * Cadastro e edição da escola: dados, modelo de turno (integral ou manhã, tarde e noite)
 * e, opcional, o horário de envio de cada turno do modelo. A grade de horários das
 * aulas é montada em Turnos e envio.
 * O modelo só muda se a escola não tiver turmas no ano atual com turno do outro modelo.
 */
export async function salvarEscola(dados) {
  exigirPerfil('admin');
  const { id, cie, nome, municipio } = dados;
  const modeloTurno = dados.modeloTurno === 'integral' ? 'integral' : 'parcial';
  const codigo = String(cie || '').trim();
  const texto = limparEspacos(nome);
  const erros = {
    cie: !codigo ? 'Informe o código CIE.' : !CIE_RE.test(codigo) ? 'O CIE tem 6 números.' : '',
    nome: !texto ? 'Informe o nome da escola.' : '',
    modeloTurno: !MODELOS_TURNO[dados.modeloTurno] ? 'Escolha o modelo de turno.' : '',
  };
  const anterior = id ? await api.escolas.obter(id) : null;
  const horarioEnvio = { ...(anterior?.horarioEnvio || {}) };
  for (const turno of turnosDoModelo(modeloTurno)) {
    const campoEnvio = `envio_${turno}`;
    try { horarioEnvio[turno] = validarHorario(dados[campoEnvio], campoEnvio); } catch (e) { Object.assign(erros, e.campos); }
  }
  validar(erros);
  if ((await api.escolas.listar({ cie: codigo })).some((e) => e.id !== id)) validar({ cie: 'Já existe uma escola com este CIE.' });
  if (anterior && (anterior.modeloTurno || 'parcial') !== modeloTurno) {
    const cfg = await api.configuracao();
    const turmas = await api.turmas.listar({ escolaId: id, ano: cfg.anoAtual });
    const permitidos = turnosDoModelo(modeloTurno);
    if (turmas.some((t) => !permitidos.includes(t.turno))) {
      validar({ modeloTurno: `A escola tem turmas de ${cfg.anoAtual} em turnos do modelo atual. Para mudar o modelo, as turmas precisam estar no turno do novo modelo.` });
    }
  }
  const registro = { cie: codigo, nome: texto, municipio: limparEspacos(municipio), modeloTurno, horarioEnvio };
  return id ? api.escolas.atualizar(id, registro) : api.escolas.criar(registro);
}

// ---------- Usuários ----------

export async function listarUsuarios({ q = '' } = {}) {
  exigirPerfil('admin');
  const escolas = indexarPor(await api.escolas.listar());
  const termo = normalizar(q);
  return (await api.usuarios.listar())
    .filter((u) => !termo || normalizar(u.nome).includes(termo) || u.login.includes(termo))
    .sort((a, b) => (a.perfil === b.perfil ? porNome(a, b) : a.perfil === 'admin' ? -1 : 1))
    .map((usuario) => ({ usuario, escola: escolas.get(usuario.escolaId) || null }));
}

export async function criarUsuario({ nome, login, escolaId, senha, senha2 }) {
  exigirPerfil('admin');
  const texto = limparEspacos(nome);
  const codigo = normalizar(login);
  validar({
    nome: !texto ? 'Informe o nome.' : '',
    login: !codigo ? 'Informe o login.' : !LOGIN_RE.test(codigo) ? 'Use de 4 a 40 caracteres: letras minúsculas, números e ponto.' : '',
    escolaId: !escolaId ? 'Escolha a escola. Todo usuário da secretaria pertence a uma escola.' : '',
    senha: !senhaValida(senha || '') ? 'Use pelo menos 8 caracteres, com letras e números.' : '',
    senha2: senha && senha !== senha2 ? 'As senhas não conferem.' : '',
  });
  if ((await api.usuarios.listar({ login: codigo })).length) validar({ login: 'Este login já está em uso.' });
  const usuario = await api.usuarios.criar({ nome: texto, login: codigo, perfil: 'secretaria', escolaId, ativo: true, falhas: 0, bloqueadoAte: null });
  await api.definirSenha(usuario.id, senha);
  return usuario;
}

export async function editarUsuario(id, { nome, escolaId, ativo }) {
  exigirPerfil('admin');
  const usuario = await api.usuarios.obter(id);
  if (!usuario) throw new NaoEncontrado();
  if (usuario.perfil !== 'secretaria') throw new RegraNegocio('Contas da Equipe Nitrix não são editadas por esta tela.');
  const texto = limparEspacos(nome);
  validar({ nome: !texto ? 'Informe o nome.' : '', escolaId: !escolaId ? 'Escolha a escola.' : '' });
  return api.usuarios.atualizar(id, { nome: texto, escolaId, ativo: ativo === '1' });
}

export async function definirNovaSenha(id, { senha, senha2 }) {
  exigirPerfil('admin');
  validar({ senha: !senhaValida(senha || '') ? 'Use pelo menos 8 caracteres, com letras e números.' : '', senha2: senha !== senha2 ? 'As senhas não conferem.' : '' });
  await api.definirSenha(id, senha);
}

export async function desbloquearUsuario(id) {
  exigirPerfil('admin');
  return api.usuarios.atualizar(id, { bloqueadoAte: null, falhas: 0 });
}

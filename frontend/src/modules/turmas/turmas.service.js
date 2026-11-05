// Turmas (somente o administrador cadastra e altera; a secretaria consulta).
// As aulas de cada dia vêm do turno da turma na escola (tela Turnos e envio).

import { api } from '../../data/api.js';
import { exigirPerfil, exigirEscola, escolaDoUsuario } from '../../app/session.js';
import { TURNOS, MODELOS_TURNO, turnosDaEscola } from '../../app/constantes.js';
import { indexarPor, porNome } from '../../utils/listas.js';
import { normalizar, limparEspacos } from '../../utils/texto.js';
import { validar, RegraNegocio, NaoEncontrado } from '../../utils/validacao.js';
import { carregarBase, frequenciaDe } from '../frequencia/frequencia.service.js';
import { mapaDeAulas, aulasDaSemana, totalSemana, semanaVigente } from '../configuracao/configuracao.service.js';

export async function listarTurmas({ ano = '', escola = '' } = {}) {
  const cfg = await api.configuracao();
  const anoEscolhido = Number(ano || cfg.anoAtual);
  const escolaId = escolaDoUsuario() || escola || undefined;
  const turmas = (await api.turmas.listar({ ano: anoEscolhido, escolaId })).sort(porNome);
  const escolas = indexarPor(await api.escolas.listar());
  const matriculas = await api.matriculas.listar({ turmaId: turmas.map((t) => t.id), status: 'ativa' });
  const aulas = await mapaDeAulas(escolaId);
  return {
    cfg,
    anoEscolhido,
    linhas: turmas.map((turma) => ({
      turma,
      escola: escolas.get(turma.escolaId),
      alunosAtivos: matriculas.filter((m) => m.turmaId === turma.id).length,
      aulasSemana: aulas.obter(turma.escolaId, turma.turno) ? totalSemana(semanaVigente(aulas.obter(turma.escolaId, turma.turno))) : null,
    })),
  };
}

export async function detalheTurma(id) {
  const turma = await api.turmas.obter(id);
  if (!turma) throw new NaoEncontrado();
  exigirEscola(turma.escolaId);
  const cfg = await api.configuracao();
  const base = await carregarBase({ escolaId: turma.escolaId });
  const matriculas = await api.matriculas.listar({ turmaId: id });
  const alunos = indexarPor(await api.alunos.listar({ id: matriculas.map((m) => m.alunoId) }));
  const ativa = turma.ano === cfg.anoAtual;
  return {
    turma,
    escola: await api.escolas.obter(turma.escolaId),
    cfg,
    ativa,
    alunos: matriculas
      .map((m) => ({ matricula: m, aluno: alunos.get(m.alunoId), freq: frequenciaDe(m, base) }))
      .sort((a, b) => porNome(a.aluno, b.aluno)),
    semana: semanaVigente(await aulasDaSemana(turma.escolaId, turma.turno)),
  };
}

export async function salvarTurma({ id, nome, turno, escolaId }) {
  exigirPerfil('admin');
  const cfg = await api.configuracao();
  const texto = limparEspacos(nome);
  validar({
    nome: !texto ? 'Informe o nome da turma.' : '',
    turno: !TURNOS[turno] ? 'Escolha o turno.' : '',
    escolaId: !escolaId ? 'Escolha a escola.' : '',
  });
  const existente = id ? await api.turmas.obter(id) : null;
  if (id && !existente) throw new NaoEncontrado();
  if (existente && existente.ano !== cfg.anoAtual) throw new RegraNegocio('Turmas de anos letivos encerrados não podem ser alteradas.');
  const ano = existente ? existente.ano : cfg.anoAtual;
  const escolaFinal = existente ? existente.escolaId : escolaId;
  const escola = await api.escolas.obter(escolaFinal);
  if (!turnosDaEscola(escola).includes(turno)) {
    validar({ turno: `Esta escola usa o modelo "${MODELOS_TURNO[escola.modeloTurno || 'parcial']}". Escolha ${escola.modeloTurno === 'integral' ? 'o turno Integral' : 'manhã, tarde ou noite'}.` });
  }
  const mesmasTurmas = await api.turmas.listar({ escolaId: escolaFinal, ano });
  if (mesmasTurmas.some((t) => t.id !== id && normalizar(t.nome) === normalizar(texto))) {
    validar({ nome: `Já existe a turma ${texto} nesta escola em ${ano}.` });
  }
  if (existente) return api.turmas.atualizar(id, { nome: texto, turno });
  return api.turmas.criar({ nome: texto, turno, escolaId: escolaFinal, ano });
}

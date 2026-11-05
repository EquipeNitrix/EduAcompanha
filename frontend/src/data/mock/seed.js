// Dados de exemplo da demonstração. São gerados de forma determinística
// (mesma "semente"), então todo mundo da equipe vê os mesmos alunos.
// Nada aqui é dado real.

import { hashSenha } from './senha.js';
import { somarDias, diaSemana, hoje, ehDiaUtil, pad } from '../../utils/datas.js';
import { rotuloTelefone } from '../../utils/formatos.js';
import { criarNotificacao, avancar } from '../../modules/notificacao/fila.js';

function sorteador(semente) {
  let a = semente;
  return () => {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Configuração das escolas: modelo de turno, horário de envio das mensagens de falta
// e grade de horários das aulas de cada dia (segunda a sexta), com data de vigência.
// A primeira escola é integral e usa a grade real da E.E. José Roque: em 03/08/2026 a
// terça e a sexta passaram a ter 9 aulas (antes eram 8, como nos outros dias). As
// ocorrências de antes dessa data continuam calculadas com a grade antiga.
const aula = (inicio, fim) => ({ inicio, fim });
const SEG_QUA_QUI = [aula('07:30', '08:20'), aula('08:20', '09:10'), aula('09:50', '10:40'), aula('10:40', '11:30'), aula('11:30', '12:20'), aula('13:45', '14:35'), aula('14:35', '15:25'), aula('15:40', '16:30')];
const TER_SEX = [aula('07:30', '08:20'), aula('08:20', '09:10'), aula('09:25', '10:15'), aula('10:15', '11:05'), aula('11:05', '11:55'), aula('12:55', '13:45'), aula('13:45', '14:35'), aula('14:35', '15:25'), aula('15:40', '16:30')];
const MANHA_HELENA = [aula('07:15', '08:05'), aula('08:05', '08:55'), aula('08:55', '09:45'), aula('10:05', '10:55'), aula('10:55', '11:45')];
const TARDE_HELENA = [aula('13:15', '14:05'), aula('14:05', '14:55'), aula('14:55', '15:45'), aula('16:05', '16:55'), aula('16:55', '17:45')];
const semana = (seg, ter, qua, qui, sex) => ({ 1: seg, 2: ter, 3: qua, 4: qui, 5: sex });

const ESCOLAS_CONFIG = {
  esc_1: {
    modeloTurno: 'integral',
    envio: { integral: '09:30' },
    grade: {
      integral: [
        { vigencia: '2025-02-03', dias: semana(SEG_QUA_QUI, SEG_QUA_QUI, SEG_QUA_QUI, SEG_QUA_QUI, SEG_QUA_QUI) },
        { vigencia: '2026-08-03', dias: semana(SEG_QUA_QUI, TER_SEX, SEG_QUA_QUI, SEG_QUA_QUI, TER_SEX) },
      ],
    },
  },
  esc_2: {
    modeloTurno: 'parcial',
    envio: { manha: '08:55', tarde: null, noite: null },
    grade: {
      manha: [{ vigencia: '2026-02-02', dias: semana(MANHA_HELENA, MANHA_HELENA, MANHA_HELENA, MANHA_HELENA, MANHA_HELENA) }],
      tarde: [{ vigencia: '2026-02-02', dias: semana(TARDE_HELENA, TARDE_HELENA, TARDE_HELENA, TARDE_HELENA, TARDE_HELENA) }],
    },
  },
};
const NOMES = ['Ana Clara', 'Arthur', 'Beatriz', 'Bernardo', 'Camila', 'Davi', 'Eduarda', 'Enzo', 'Fernanda', 'Gabriel', 'Giovanna', 'Heitor', 'Isabela', 'João Pedro', 'Júlia', 'Kauã', 'Larissa', 'Lorenzo', 'Manuela', 'Matheus', 'Nicole', 'Otávio', 'Pietra', 'Rafael', 'Sophia', 'Thiago', 'Valentina', 'Vinícius', 'Yasmin', 'Lucas Gabriel'];
const SOBRENOMES = ['Almeida', 'Barbosa', 'Cardoso', 'Costa', 'Dias', 'Ferreira', 'Gomes', 'Lima', 'Martins', 'Moreira', 'Nascimento', 'Oliveira', 'Pereira', 'Ribeiro', 'Rocha', 'Santos', 'Silva', 'Souza', 'Teixeira', 'Vieira'];
const MAES = ['Adriana', 'Cláudia', 'Daniela', 'Elaine', 'Fabiana', 'Juliana', 'Kátia', 'Patrícia', 'Renata', 'Simone', 'Tatiane', 'Vanessa'];
const PAIS = ['Anderson', 'Carlos', 'Edson', 'Fábio', 'Marcelo', 'Ricardo', 'Rogério', 'Sérgio'];
const RESPOSTAS = ['Estava com febre, levei ao posto de saúde.', 'Teve consulta médica, vou levar o atestado.', 'Perdeu o ônibus escolar hoje cedo.', 'Problema familiar, amanhã ele volta.', 'Estava com dor de garganta, ficou em casa.'];

export function gerarDadosIniciais() {
  const R = sorteador(20261015);
  const sortear = (lista) => lista[Math.floor(R() * lista.length)];
  const dataHoje = hoje();
  const ontem = somarDias(dataHoje, -1);
  const agora = Date.now();
  let seq = 0;
  const novoId = (prefixo) => `${prefixo}_${(++seq).toString(36)}`;

  const config = {
    anoAtual: 2026,
    anosLetivos: {
      2025: { inicio: '2025-02-03', fim: '2025-12-19' },
      2026: { inicio: '2026-02-02', fim: '2026-12-18' },
    },
  };
  const c = {
    escolas: [], usuarios: [], horarios: [], turmas: [], alunos: [], matriculas: [],
    responsaveis: [], telefones: [], lancamentos: [], notificacoes: [], casos: [], acompanhamentos: [],
  };

  // ---------- Escolas e usuários ----------
  c.escolas.push(
    { id: 'esc_1', cie: '035262', nome: 'E.E. Professor José Roque Almeida Rosa', municipio: 'Sorocaba', modeloTurno: 'integral', horarioEnvio: { ...ESCOLAS_CONFIG.esc_1.envio } },
    { id: 'esc_2', cie: '035419', nome: 'E.E. Professora Helena Duarte Vieira', municipio: 'Sorocaba', modeloTurno: 'parcial', horarioEnvio: { ...ESCOLAS_CONFIG.esc_2.envio } },
  );
  const usuario = (id, login, nome, perfil, escolaId, senha) => ({ id, login, nome, perfil, escolaId, senhaHash: hashSenha(senha), ativo: true, falhas: 0, bloqueadoAte: null });
  c.usuarios.push(
    usuario('usr_admin', 'admin.nitrix', 'Equipe Nitrix', 'admin', null, 'Nitrix@2026'),
    usuario('usr_luciene', 'conta.sec1', 'Conta.sec1', 'secretaria', 'esc_1', 'Escola@2026'),
    usuario('usr_marcos', 'conta.sec2', 'Conta.sec2', 'secretaria', 'esc_2', 'Escola@2026'),
  );
  const secretariaDe = (escolaId) => (escolaId === 'esc_1' ? 'usr_luciene' : 'usr_marcos');

  // ---------- Grade de horários de cada turno ----------
  for (const [escolaId, { grade }] of Object.entries(ESCOLAS_CONFIG)) {
    for (const [turno, versoes] of Object.entries(grade)) {
      for (const { vigencia, dias } of versoes) {
        for (const [dia, aulas] of Object.entries(dias)) {
          aulas.forEach((a, i) => c.horarios.push({ id: `hor_${escolaId}_${turno}_${vigencia}_${dia}_${i + 1}`, escolaId, turno, dia: Number(dia), ordem: i + 1, inicio: a.inicio, fim: a.fim, vigencia }));
        }
      }
    }
  }

  // ---------- Turmas ----------
  c.turmas.push(
    { id: 'tur_0', escolaId: 'esc_1', nome: '6º A', turno: 'integral', ano: 2025 },
    { id: 'tur_1', escolaId: 'esc_1', nome: '7º A', turno: 'integral', ano: 2026 },
    { id: 'tur_2', escolaId: 'esc_1', nome: '8º B', turno: 'integral', ano: 2026 },
    { id: 'tur_3', escolaId: 'esc_1', nome: '1ª série A', turno: 'integral', ano: 2026 },
    { id: 'tur_4', escolaId: 'esc_2', nome: '6º A', turno: 'manha', ano: 2026 },
  );
  const turmaPorId = (id) => c.turmas.find((t) => t.id === id);
  /** Aulas do turno da turma naquele dia ([] = sem aula). */
  function aulasEm(turmaId, data) {
    const t = turmaPorId(turmaId);
    const versoes = ESCOLAS_CONFIG[t.escolaId].grade[t.turno] || [];
    const vigente = versoes.filter((v) => v.vigencia <= data).at(-1);
    const w = diaSemana(data);
    return vigente && w >= 1 && w <= 5 ? vigente.dias[w] : [];
  }
  const somarMin = (hora, min) => { const [h, m] = hora.split(':').map(Number); const t = h * 60 + m + min; return `${pad(Math.floor(t / 60))}:${pad(t % 60)}`; };

  // ---------- Alunos, matrículas, responsáveis e telefones ----------
  const telefone = () => `(15) 9${Math.floor(7000 + R() * 2999)}-${pad(Math.floor(R() * 100))}${pad(Math.floor(R() * 100))}`;
  const plano = [['tur_1', 8], ['tur_2', 8], ['tur_3', 8], ['tur_4', 6]];
  const porTurma = {};
  const taxaFalta = {};
  let k = 0;
  for (const [turmaId, quantidade] of plano) {
    porTurma[turmaId] = [];
    for (let i = 0; i < quantidade; i++) {
      const sobrenome = `${sortear(SOBRENOMES)} ${sortear(SOBRENOMES)}`;
      const aluno = { id: `alu_${k + 1}`, ra: `000${110000000 + Math.floor(R() * 89999999)}-${Math.floor(R() * 10)}`, nome: `${NOMES[k]} ${sobrenome}` };
      c.alunos.push(aluno);
      porTurma[turmaId].push(aluno.id);

      const inicio = turmaId === 'tur_2' && i === 7 ? '2026-05-04' : '2026-02-02';
      c.matriculas.push({ id: `mat_${k + 1}`, alunoId: aluno.id, turmaId, ano: 2026, inicio, fim: null, status: 'ativa' });
      if (turmaId === 'tur_1') {
        c.matriculas.push({ id: `mat_a${k + 1}`, alunoId: aluno.id, turmaId: 'tur_0', ano: 2025, inicio: '2025-02-03', fim: '2025-12-19', status: 'encerrada' });
      }

      const ultimoSobrenome = sobrenome.split(' ')[1];
      const mae = R() < 0.78;
      const r1 = { id: novoId('rsp'), alunoId: aluno.id, nome: `${mae ? sortear(MAES) : sortear(PAIS)} ${ultimoSobrenome}`, parentesco: mae ? 'Mãe' : 'Pai' };
      c.responsaveis.push(r1);
      const tels = [{ responsavelId: r1.id, numero: telefone() }];
      if (R() < 0.35) {
        const r2 = { id: novoId('rsp'), alunoId: aluno.id, nome: `${sortear(['Maria', 'Aparecida', 'José', 'Antônio'])} ${sobrenome.split(' ')[0]}`, parentesco: sortear(['Avó', 'Avô', 'Tia']) };
        c.responsaveis.push(r2);
        tels.push({ responsavelId: r2.id, numero: telefone() });
      }
      if (R() < 0.6) tels.push({ responsavelId: r1.id, numero: telefone() });
      tels.forEach((t, ordem) => c.telefones.push({ id: novoId('tel'), alunoId: aluno.id, responsavelId: t.responsavelId, numero: t.numero, ordem }));

      taxaFalta[aluno.id] = 0.02 + R() * 0.045;
      k++;
    }
  }
  const A = (turmaId, i) => porTurma[turmaId][i];
  Object.assign(taxaFalta, {
    [A('tur_1', 2)]: 0.31, [A('tur_2', 5)]: 0.28, [A('tur_3', 1)]: 0.34, [A('tur_4', 3)]: 0.29,
    [A('tur_1', 6)]: 0.2, [A('tur_3', 4)]: 0.21,
  });

  // Telefone Principal inválido (termina em 0000): a mensagem passa direto para o Secundário.
  const alunoNumeroInvalido = A('tur_1', 2);
  const telsInvalido = c.telefones.filter((t) => t.alunoId === alunoNumeroInvalido).sort((a, b) => a.ordem - b.ordem);
  telsInvalido[0].numero = '(15) 99123-0000';
  if (telsInvalido.length < 2) c.telefones.push({ id: novoId('tel'), alunoId: alunoNumeroInvalido, responsavelId: telsInvalido[0].responsavelId, numero: telefone(), ordem: 1 });

  // ---------- Lançamentos (uma chamada por dia) ----------
  function lancar(m, data, tipo, { horaEntrada = null, horaSaida = null, observacao = null } = {}) {
    const t = turmaPorId(m.turmaId);
    const conferencia = somarMin(aulasEm(t.id, data)[0]?.inicio || '07:30', 15);
    c.lancamentos.push({
      id: novoId('lan'), alunoId: m.alunoId, matriculaId: m.id, turmaId: t.id, escolaId: t.escolaId, data, tipo,
      horaEntrada, horaSaida, observacao, status: 'ativo',
      criadoPor: secretariaDe(t.escolaId), criadoEm: `${data}T${tipo === 'falta' ? conferencia : (horaEntrada || horaSaida)}:00`,
      motivoCancelamento: null, canceladoEm: null, canceladoPor: null,
    });
  }
  const MOTIVOS_ATRASO = [null, null, 'Ônibus escolar atrasou.', 'Consulta odontológica pela manhã.'];
  const MOTIVOS_SAIDA = [null, 'Responsável buscou mais cedo.', 'Passou mal, a mãe buscou.'];
  // Padrão de demonstração nos últimos 30 dias: um aluno falta toda segunda-feira.
  const inicioJanela = somarDias(dataHoje, -29);
  const especiais = { [A('tur_1', 4)]: 'segundas' };
  for (const m of c.matriculas) {
    const t = turmaPorId(m.turmaId);
    const ano = config.anosLetivos[m.ano];
    const fim = m.ano === 2025 ? ano.fim : ontem;
    for (let d = m.inicio; d <= fim && d <= ano.fim; d = somarDias(d, 1)) {
      const aulas = aulasEm(t.id, d);
      if (!ehDiaUtil(d) || !aulas.length) continue;
      const especial = m.ano === 2026 ? especiais[m.alunoId] : null;
      if (especial && d >= inicioJanela) {
        if (especial === 'segundas' && diaSemana(d) === 1) lancar(m, d, 'falta');
        continue;
      }
      const n = aulas.length;
      const taxa = (taxaFalta[m.alunoId] || 0.04) * (m.ano === 2025 ? 0.8 : 1);
      if (R() < taxa) lancar(m, d, 'falta');
      else if (R() < 0.02) {
        // Atraso: chega durante a 2ª, 3ª ou 4ª aula.
        const i = 1 + Math.floor(R() * 3);
        lancar(m, d, 'atraso', { horaEntrada: somarMin(aulas[i].inicio, 10), observacao: sortear(MOTIVOS_ATRASO) });
      } else if (R() < 0.012) {
        // Saída antecipada: sai antes das 1 a 3 últimas aulas.
        const i = n - 1 - Math.floor(R() * 3);
        lancar(m, d, 'saida', { horaSaida: somarMin(aulas[i].inicio, -5), observacao: sortear(MOTIVOS_SAIDA) });
      } else if (R() < 0.006 && n >= 5) {
        // Saiu e voltou: consulta no meio do dia, perde 2 aulas.
        const i = 1 + Math.floor(R() * (n - 3));
        lancar(m, d, 'saidaRetorno', { horaSaida: somarMin(aulas[i].inicio, -5), horaEntrada: somarMin(aulas[i + 2].inicio, -5), observacao: 'Consulta médica com o responsável.' });
      }
    }
  }

  // Alguns cancelamentos, para o histórico mostrar o motivo.
  const MOTIVOS = ['Aluno estava presente; lançado na turma errada.', 'Aluno chegou atrasado depois da conferência em sala.', 'Aluno em atividade externa autorizada pela direção.'];
  const candidatas = c.lancamentos.filter((l) => l.tipo === 'falta' && l.data >= somarDias(dataHoje, -45) && l.data < somarDias(dataHoje, -12) && !especiais[l.alunoId]);
  for (let i = 0; i < 3 && candidatas.length; i++) {
    const l = candidatas.splice(Math.floor(R() * candidatas.length), 1)[0];
    Object.assign(l, { status: 'cancelado', motivoCancelamento: MOTIVOS[i], canceladoEm: `${l.data}T09:10:00`, canceladoPor: l.criadoPor });
  }

  // Faltas de hoje (se for dia útil), para demonstrar o envio automático.
  if (ehDiaUtil(dataHoje)) {
    for (const [turmaId, i] of [['tur_1', 1], ['tur_2', 2], ['tur_3', 5], ['tur_4', 0]]) {
      const m = c.matriculas.find((x) => x.alunoId === A(turmaId, i) && x.ano === 2026);
      if (!c.lancamentos.some((l) => l.matriculaId === m.id && l.data === dataHoje)) {
        lancar(m, dataHoje, 'falta');
      }
    }
    const mAtraso = c.matriculas.find((x) => x.alunoId === A('tur_1', 3) && x.ano === 2026);
    if (!c.lancamentos.some((l) => l.matriculaId === mAtraso.id && l.data === dataHoje)) lancar(mAtraso, dataHoje, 'atraso', { horaEntrada: '09:00', observacao: 'Ônibus escolar atrasou.' });
    const mConsulta = c.matriculas.find((x) => x.alunoId === A('tur_2', 4) && x.ano === 2026);
    if (!c.lancamentos.some((l) => l.matriculaId === mConsulta.id && l.data === dataHoje)) {
      lancar(mConsulta, dataHoje, 'saidaRetorno', { horaSaida: '09:30', horaEntrada: '13:00', observacao: 'Consulta médica com a mãe.' });
    }
  }

  // ---------- Notificações dos últimos dias ----------
  const filaDe = (alunoId) => c.telefones
    .filter((t) => t.alunoId === alunoId)
    .sort((a, b) => a.ordem - b.ordem)
    .map((t, i) => {
      const r = c.responsaveis.find((x) => x.id === t.responsavelId);
      return { telefoneId: t.id, numero: t.numero, nome: r.nome, parentesco: r.parentesco, rotulo: rotuloTelefone(i) };
    });
  for (let dias = 1; dias <= 10; dias++) {
    const dia = somarDias(dataHoje, -dias);
    if (!ehDiaUtil(dia)) continue;
    const faltasDia = c.lancamentos.filter((l) => l.data === dia && l.tipo === 'falta' && l.status === 'ativo');
    for (const alunoId of [...new Set(faltasDia.map((l) => l.alunoId))]) {
      const lanc = faltasDia.find((l) => l.alunoId === alunoId);
      const turma = turmaPorId(lanc.turmaId);
      const escola = c.escolas.find((e) => e.id === turma.escolaId);
      const aluno = c.alunos.find((a) => a.id === alunoId);
      const horario = ESCOLAS_CONFIG[turma.escolaId].envio[turma.turno];
      if (!horario) continue;
      const envio = new Date(`${dia}T${horario}:00`).getTime() + 2 * 60000;
      const notificacao = criarNotificacao({ aluno, turma, escola, data: dia, fila: filaDe(alunoId), enviarEm: envio });
      const sorte = R();
      let respostaEm = null;
      if (sorte < 0.55) respostaEm = envio + (1 + R() * 8) * 3600000;
      else if (sorte < 0.7) respostaEm = envio + (26 + R() * 8) * 3600000;
      if (respostaEm && respostaEm < agora) {
        avancar(notificacao, respostaEm);
        if (notificacao.status === 'aguardando') {
          Object.assign(notificacao, { status: 'respondida', resposta: sortear(RESPOSTAS), respondidoEm: new Date(respostaEm).toISOString() });
        }
      }
      if (notificacao.status === 'aguardando') avancar(notificacao, agora);
      c.notificacoes.push({ id: novoId('ntf'), ...notificacao });
    }
  }

  // ---------- Casos de Busca Ativa ----------
  c.casos.push(
    { id: 'cas_1', alunoId: A('tur_1', 2), escolaId: 'esc_1', abertura: '2026-09-21', motivo: 'Frequência abaixo de 75% e faltas seguidas em setembro.', status: 'aberto', encerramento: null, desfecho: null, observacaoFinal: '', criadoPor: 'usr_luciene' },
    { id: 'cas_2', alunoId: A('tur_2', 3), escolaId: 'esc_1', abertura: '2026-08-04', motivo: 'Cinco faltas seguidas sem justificativa.', status: 'encerrado', encerramento: '2026-09-02', desfecho: 'Aluno retornou à frequência regular', observacaoFinal: 'Frequência normalizada nas quatro semanas seguintes.', criadoPor: 'usr_luciene' },
  );
  // Tentativas de contato e ações ficam juntas, como na tabela ACOMPANHAMENTO_CASO.
  const primeiroResponsavel = (alunoId) => c.responsaveis.find((r) => r.alunoId === alunoId);
  const r1 = primeiroResponsavel(A('tur_1', 2));
  const r2 = c.responsaveis.filter((r) => r.alunoId === A('tur_2', 3)).at(-1);
  const tentativa = (id, casoId, canal, dataHora, descricao, resultado, responsavelId) => ({ id, casoId, tipoRegistro: 'tentativa', dataHora, descricao, canal, resultado, tipoAcao: null, responsavelId, registradoPor: 'usr_luciene' });
  const acao = (id, casoId, tipoAcao, dataHora, descricao) => ({ id, casoId, tipoRegistro: 'acao', dataHora, descricao, canal: null, resultado: null, tipoAcao, responsavelId: null, registradoPor: 'usr_luciene' });
  c.acompanhamentos.push(
    tentativa('acp_1', 'cas_1', 'Ligação telefônica', '2026-09-21T10:15', 'Não atendeu. Deixado recado para retornar à secretaria.', 'Sem sucesso', r1.id),
    tentativa('acp_2', 'cas_1', 'WhatsApp', '2026-09-23T14:40', 'Respondeu que o aluno está trabalhando com o tio no período da manhã. Combinado comparecimento na escola.', 'Contato efetivo', r1.id),
    acao('acp_3', 'cas_1', 'Reunião com a família', '2026-09-25T09:00', 'Reunião com a família e a coordenação. Acordo de retorno às aulas e acompanhamento semanal.'),
    tentativa('acp_4', 'cas_2', 'Visita domiciliar', '2026-08-06T09:30', 'Informou problema de transporte. Orientada sobre o transporte escolar gratuito.', 'Contato efetivo', r2.id),
    acao('acp_5', 'cas_2', 'Encaminhamento', '2026-08-07T11:00', 'Pedido de inclusão no transporte escolar junto à Diretoria de Ensino.'),
  );

  return { versao: 7, config, colecoes: c };
}

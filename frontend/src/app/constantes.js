// Valores de domínio usados em várias telas.

/** Frequência mínima exigida pela LDB (art. 24, VI). Abaixo disso o aluno está em situação crítica de evasão. */
export const LIMITE_FREQUENCIA = 75;

/** Faixa preventiva: entre LIMITE_FREQUENCIA e este valor, o aluno entra em alerta de atenção, antes de ficar crítico. */
export const LIMITE_ATENCAO = 80;

/** Horas sem resposta antes de tentar o próximo telefone da lista. */
export const PRAZO_RESPOSTA_HORAS = 24;

/** Padrões de falta: janela em dias corridos e mínimo de ocorrências. */
export const PADRAO_JANELA_DIAS = 30;
export const PADRAO_MINIMO = 3;

export const TIPOS_OCORRENCIA = { falta: 'Falta', atraso: 'Atraso', saida: 'Saída antecipada', saidaRetorno: 'Saiu e voltou' };
export const TOM_OCORRENCIA = { falta: 'crit', atraso: 'warn', saida: 'info', saidaRetorno: 'info' };

export const TURNOS = { manha: 'Manhã', tarde: 'Tarde', noite: 'Noite', integral: 'Integral' };
/** "turno da manhã", "turno integral"... para usar no meio das frases. */
export const TURNO_EXTENSO = { manha: 'turno da manhã', tarde: 'turno da tarde', noite: 'turno da noite', integral: 'turno integral' };

/** Modelo de turno da escola: integral (um turno o dia todo) ou parcial (manhã, tarde e noite). */
export const MODELOS_TURNO = { parcial: 'Manhã, tarde e noite', integral: 'Integral' };
export const turnosDoModelo = (modelo) => (modelo === 'integral' ? ['integral'] : ['manha', 'tarde', 'noite']);
export const turnosDaEscola = (escola) => turnosDoModelo(escola?.modeloTurno);

export const PARENTESCOS = ['Mãe', 'Pai', 'Avó', 'Avô', 'Tia', 'Tio', 'Irmã', 'Irmão', 'Responsável legal', 'Outro'];

export const CANAIS_CONTATO = ['Ligação telefônica', 'WhatsApp', 'Visita domiciliar', 'Reunião na escola', 'Bilhete ou carta', 'Outro'];
export const RESULTADOS_CONTATO = ['Contato efetivo', 'Sem sucesso', 'Número inválido', 'Retorno agendado'];
export const TIPOS_ACAO = ['Reunião com a família', 'Encaminhamento', 'Acordo de retorno', 'Ação pedagógica', 'Outro'];
export const DESFECHOS_CASO = ['Aluno retornou à frequência regular', 'Aluno transferido de escola', 'Encaminhado à rede de proteção', 'Aluno não localizado', 'Outro'];

/** status da notificação -> [texto, tom do chip] */
export const STATUS_NOTIFICACAO = {
  aguardando: ['Aguardando resposta', 'warn'],
  respondida: ['Entregue com resposta', 'ok'],
  sem_resposta: ['Encerrada sem resposta', 'crit'],
  interrompida: ['Interrompida', 'muted'],
};

-- EduAcompanha - Banco de dados (MySQL 8.0.16)
-- Equipe Nitrix - UPX II - UniFacens
-- Versão de 04/11/2026
-- Nomes das restrições: uq_ = UNIQUE, ck_ = CHECK, idx_ = índice, fk_ = chave estrangeira.
USE eduacompanha;


-- TABELAS SEM DEPENDÊNCIAS


-- Horário de envio das mensagens de falta, um por turno. NULL = turno sem envio
CREATE TABLE ESCOLA (
    CIE CHAR(6) NOT NULL PRIMARY KEY,
    nome_escola VARCHAR(150) NOT NULL,
    municipio VARCHAR(60) NULL,
    horario_envio_manha TIME NULL,
    horario_envio_tarde TIME NULL,
    horario_envio_noite TIME NULL,
    CONSTRAINT ck_escola_cie CHECK (CIE REGEXP '^[0-9]{6}$')
) ENGINE=InnoDB;

-- Ano letivo: início e fim usados na contagem das aulas previstas
CREATE TABLE ANO_LETIVO (
    ano INT NOT NULL PRIMARY KEY,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    CONSTRAINT ck_ano_letivo_datas CHECK (data_fim > data_inicio)
) ENGINE=InnoDB;


CREATE TABLE ALUNO (
    id_aluno INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    RA CHAR(13) NOT NULL,
    nome_aluno VARCHAR(120) NOT NULL,
    CONSTRAINT uq_aluno_ra UNIQUE (RA),
    CONSTRAINT ck_aluno_ra CHECK (RA REGEXP '^[0-9]{12}[0-9X]$')
) ENGINE=InnoDB;



-- TABELAS COM DEPENDÊNCIAS

CREATE TABLE USUARIO (
    id_usuario INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    login VARCHAR(40) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(15) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    tentativas_falhas INT NOT NULL DEFAULT 0,
    bloqueado_ate DATETIME NULL,
    CIE CHAR(6) NULL,
    CONSTRAINT uq_usuario_login UNIQUE (login),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'SECRETARIA')),
    CONSTRAINT ck_usuario_perfil_escola CHECK ((perfil = 'ADMIN' AND CIE IS NULL) OR (perfil = 'SECRETARIA' AND CIE IS NOT NULL)),
    CONSTRAINT ck_usuario_tentativas CHECK (tentativas_falhas >= 0)
) ENGINE=InnoDB;


-- O turno define qual horário de envio da escola vale para os alunos dela.
CREATE TABLE TURMA (
    id_turma INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome_turma VARCHAR(30) NOT NULL,
    turno VARCHAR(10) NOT NULL,
    ano_letivo INT NOT NULL,
    CIE CHAR(6) NOT NULL,
    CONSTRAINT uq_turma_escola_nome_ano UNIQUE (CIE, nome_turma, ano_letivo),
    CONSTRAINT ck_turma_turno CHECK (turno IN ('MANHA', 'TARDE', 'NOITE'))
) ENGINE=InnoDB;

-- Quantidade de aulas em cada dia da semana, por escola e por turno
-- Base do cálculo da frequência em aulas.
-- data_vigencia: a partir de quando os números valem. Uma mudança cria linhas novas com nova data; as antigas continuam valendo para os dias anteriores,
-- então a frequência já calculada não muda. Para uma data, vale a linha com a
-- maior data_vigencia menor ou igual a ela.
CREATE TABLE AULAS_DIA (
    id_aulas_dia INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    CIE CHAR(6) NOT NULL,
    turno VARCHAR(10) NOT NULL,
    dia_semana INT NOT NULL,
    qtd_aulas INT NOT NULL,
    data_vigencia DATE NOT NULL,
    CONSTRAINT uq_aulas_dia_escola_turno_dia_vigencia UNIQUE (CIE, turno, dia_semana, data_vigencia),
    CONSTRAINT ck_aulas_dia_turno CHECK (turno IN ('MANHA', 'TARDE', 'NOITE')),
    CONSTRAINT ck_aulas_dia_dia CHECK (dia_semana BETWEEN 1 AND 5),
    CONSTRAINT ck_aulas_dia_qtd CHECK (qtd_aulas BETWEEN 0 AND 10)
) ENGINE=InnoDB;

-- Responsável: pertence a um aluno. Se dois irmãos têm a mesma mãe,
-- ela é cadastrada em cada um deles.
CREATE TABLE RESPONSAVEL (
    id_responsavel INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome_responsavel VARCHAR(120) NOT NULL,
    parentesco VARCHAR(20) NOT NULL,
    id_aluno INT NOT NULL
) ENGINE=InnoDB;

-- Telefone de um responsável (nunca do aluno).
-- posicao é a ordem de contato do aluno: 1 = Principal, 2 = Secundário, 3 = Secundário 2
CREATE TABLE CONTATO_RESPONSAVEL (
    id_contato INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    telefone VARCHAR(15) NOT NULL,
    posicao INT NOT NULL,
    id_responsavel INT NOT NULL,
    CONSTRAINT uq_contato_responsavel_telefone UNIQUE (id_responsavel, telefone),
    CONSTRAINT ck_contato_posicao CHECK (posicao >= 1)
) ENGINE=InnoDB;

-- Matrícula: liga o aluno à turma e guarda o histórico (as encerradas não são apagadas)
CREATE TABLE MATRICULA (
    id_matricula INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_aluno INT NOT NULL,
    id_turma INT NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ATIVA',
    CONSTRAINT ck_matricula_status CHECK (status IN ('ATIVA', 'ENCERRADA')),
    CONSTRAINT ck_matricula_datas CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    -- Matrícula encerrada precisa ter data de fim
    CONSTRAINT ck_matricula_encerrada CHECK (status = 'ATIVA' OR data_fim IS NOT NULL)
) ENGINE=InnoDB;


-- Uma falta vale para todas as aulas do dia, conforme AULAS_DIA da escola
-- vigente na data, para o turno da turma e o dia da semana da falta.
-- O registro nunca é apagado: é cancelado com motivo.
CREATE TABLE REGISTRO_FREQUENCIA (
    id_registro_freq INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    data DATE NOT NULL,
    tipo_ocorrencia VARCHAR(20) NOT NULL,
    id_matricula INT NOT NULL,
    registrado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_registro INT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
    motivo_cancelamento VARCHAR(300) NULL,
    cancelado_em DATETIME NULL,
    id_usuario_cancelamento INT NULL,
    CONSTRAINT ck_registro_tipo CHECK (tipo_ocorrencia IN ('FALTA', 'ATRASO', 'SAIDA_ANTECIPADA')),
    CONSTRAINT ck_registro_status CHECK (status IN ('ATIVO', 'CANCELADO')),
    -- Ativo: sem dados de cancelamento. Cancelado: motivo, data e usuário obrigatórios.
    CONSTRAINT ck_registro_cancelamento CHECK (
        (status = 'ATIVO' AND motivo_cancelamento IS NULL AND cancelado_em IS NULL AND id_usuario_cancelamento IS NULL)
        OR (status = 'CANCELADO' AND motivo_cancelamento IS NOT NULL AND cancelado_em IS NOT NULL AND id_usuario_cancelamento IS NOT NULL)
    ),
    -- Histórico do aluno e tela de lançamento (registros de uma matrícula por data)
    INDEX idx_registro_matricula_data (id_matricula, data),
    -- Rotina diária de envio: faltas ativas do dia
    INDEX idx_registro_data_tipo_status (data, tipo_ocorrencia, status)
) ENGINE=InnoDB;

-- Mensagem automática de falta pelo WhatsApp (uma por aluno por dia)
CREATE TABLE NOTIFICACAO (
    id_notificacao INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_matricula INT NOT NULL,
    data_falta DATE NOT NULL,
    mensagem VARCHAR(500) NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'AGUARDANDO',
    resposta VARCHAR(500) NULL,
    respondida_em DATETIME NULL,
    encerrada_em DATETIME NULL,
    observacao VARCHAR(255) NULL,
    CONSTRAINT uq_notificacao_matricula_dia UNIQUE (id_matricula, data_falta),
    CONSTRAINT ck_notificacao_status CHECK (status IN ('AGUARDANDO', 'RESPONDIDA', 'SEM_RESPOSTA', 'INTERROMPIDA')),
    -- Rotina de escalonamento: notificações aguardando resposta
    INDEX idx_notificacao_status (status)
) ENGINE=InnoDB;

-- Cada envio da fila de contato. telefone_enviado guarda o número usado,
-- mesmo que o contato seja editado ou removido depois.
CREATE TABLE ENVIO_NOTIFICACAO (
    id_envio INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_notificacao INT NOT NULL,
    id_contato INT NULL,
    posicao INT NOT NULL,
    telefone_enviado VARCHAR(15) NOT NULL,
    enviado_em DATETIME NOT NULL,
    entregue BOOLEAN NOT NULL,
    CONSTRAINT uq_envio_notificacao_posicao UNIQUE (id_notificacao, posicao),
    CONSTRAINT ck_envio_posicao CHECK (posicao >= 1)
) ENGINE=InnoDB;

-- Caso de Busca Ativa Escolar (aberto manualmente pela secretaria)
CREATE TABLE CASO_BUSCA_ATIVA (
    id_caso INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    data_abertura DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ABERTO',
    data_encerramento DATE NULL,
    id_aluno INT NOT NULL,
    CIE CHAR(6) NOT NULL,
    motivo VARCHAR(500) NULL,
    desfecho VARCHAR(60) NULL,
    observacao_final VARCHAR(500) NULL,
    id_usuario_abertura INT NOT NULL,
    CONSTRAINT ck_caso_status CHECK (status IN ('ABERTO', 'ENCERRADO')),
    -- Aberto: sem encerramento. Encerrado: data e desfecho obrigatórios.
    CONSTRAINT ck_caso_encerramento CHECK (
        (status = 'ABERTO' AND data_encerramento IS NULL)
        OR (status = 'ENCERRADO' AND data_encerramento IS NOT NULL AND desfecho IS NOT NULL)
    ),
    CONSTRAINT ck_caso_datas CHECK (data_encerramento IS NULL OR data_encerramento >= data_abertura),
    -- Painel de Busca Ativa: casos por escola e situação
    INDEX idx_caso_escola_status (CIE, status)
) ENGINE=InnoDB;

-- Acompanhamento do caso: guarda as tentativas de contato com a família
-- (tipo_registro = TENTATIVA) e as ações e encaminhamentos (tipo_registro = ACAO).
-- canal, resultado e id_responsavel são só da tentativa; tipo_acao é só da ação.
CREATE TABLE ACOMPANHAMENTO_CASO (
    id_acompanhamento INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tipo_registro VARCHAR(10) NOT NULL,
    data_hora DATETIME NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    canal VARCHAR(30) NULL,
    resultado VARCHAR(30) NULL,
    tipo_acao VARCHAR(40) NULL,
    id_caso INT NOT NULL,
    id_responsavel INT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT ck_acompanhamento_tipo CHECK (tipo_registro IN ('TENTATIVA', 'ACAO')),
    -- Tentativa: canal e resultado obrigatórios, sem tipo de ação.
    -- Ação: tipo de ação obrigatório, sem canal e sem resultado.
    -- id_responsavel fica de fora do CHECK porque o MySQL não aceita CHECK
    -- em coluna de chave estrangeira com ON DELETE SET NULL (regra no sistema).
    CONSTRAINT ck_acompanhamento_campos CHECK (
        (tipo_registro = 'TENTATIVA' AND canal IS NOT NULL AND resultado IS NOT NULL AND tipo_acao IS NULL)
        OR (tipo_registro = 'ACAO' AND tipo_acao IS NOT NULL AND canal IS NULL AND resultado IS NULL)
    ),
    -- Linha do tempo do caso
    INDEX idx_acompanhamento_caso_data (id_caso, data_hora)
) ENGINE=InnoDB;


-- CHAVES ESTRANGEIRAS
-- RESTRICT (não deixa apagar um registro que ainda é usado).


ALTER TABLE USUARIO
    ADD CONSTRAINT fk_usuario_escola FOREIGN KEY (CIE) REFERENCES ESCOLA (CIE) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE TURMA
    ADD CONSTRAINT fk_turma_escola FOREIGN KEY (CIE) REFERENCES ESCOLA (CIE) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_turma_ano_letivo FOREIGN KEY (ano_letivo) REFERENCES ANO_LETIVO (ano) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE AULAS_DIA
    ADD CONSTRAINT fk_aulas_dia_escola FOREIGN KEY (CIE) REFERENCES ESCOLA (CIE) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE RESPONSAVEL
    ADD CONSTRAINT fk_responsavel_aluno FOREIGN KEY (id_aluno) REFERENCES ALUNO (id_aluno) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE CONTATO_RESPONSAVEL
    ADD CONSTRAINT fk_contato_responsavel FOREIGN KEY (id_responsavel) REFERENCES RESPONSAVEL (id_responsavel) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE MATRICULA
    ADD CONSTRAINT fk_matricula_aluno FOREIGN KEY (id_aluno) REFERENCES ALUNO (id_aluno) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_matricula_turma FOREIGN KEY (id_turma) REFERENCES TURMA (id_turma) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE REGISTRO_FREQUENCIA
    ADD CONSTRAINT fk_registro_matricula FOREIGN KEY (id_matricula) REFERENCES MATRICULA (id_matricula) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_registro_usuario_registro FOREIGN KEY (id_usuario_registro) REFERENCES USUARIO (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_registro_usuario_cancelamento FOREIGN KEY (id_usuario_cancelamento) REFERENCES USUARIO (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE NOTIFICACAO
    ADD CONSTRAINT fk_notificacao_matricula FOREIGN KEY (id_matricula) REFERENCES MATRICULA (id_matricula) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Se o telefone for removido depois do envio, o histórico continua
-- (o número fica em telefone_enviado) e a ligação vira nula.
ALTER TABLE ENVIO_NOTIFICACAO
    ADD CONSTRAINT fk_envio_notificacao FOREIGN KEY (id_notificacao) REFERENCES NOTIFICACAO (id_notificacao) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_envio_contato FOREIGN KEY (id_contato) REFERENCES CONTATO_RESPONSAVEL (id_contato) ON DELETE SET NULL ON UPDATE RESTRICT;

ALTER TABLE CASO_BUSCA_ATIVA
    ADD CONSTRAINT fk_caso_aluno FOREIGN KEY (id_aluno) REFERENCES ALUNO (id_aluno) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_caso_escola FOREIGN KEY (CIE) REFERENCES ESCOLA (CIE) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_caso_usuario FOREIGN KEY (id_usuario_abertura) REFERENCES USUARIO (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Se o responsável for removido do cadastro, a tentativa continua registrada
-- e a ligação com ele vira nula.
ALTER TABLE ACOMPANHAMENTO_CASO
    ADD CONSTRAINT fk_acompanhamento_caso FOREIGN KEY (id_caso) REFERENCES CASO_BUSCA_ATIVA (id_caso) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_acompanhamento_responsavel FOREIGN KEY (id_responsavel) REFERENCES RESPONSAVEL (id_responsavel) ON DELETE SET NULL ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_acompanhamento_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT;

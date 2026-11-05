# EduAcompanha · Front-end

Plataforma web de acompanhamento de frequência escolar.
Equipe Nitrix · UPX II · Centro Universitário Facens · Orientador: Eliney Sabino.

> **Modo demonstração:** ainda não há back-end. Os dados ficam salvos no navegador
> (localStorage) e o envio de WhatsApp é simulado. Nada aqui é dado real.

---

## Como rodar

**Opção 1: Vite (recomendado)**

Precisa do [Node.js](https://nodejs.org) 18 ou mais novo.

```bash
npm install
npm run dev
```

O navegador abre em `http://localhost:5173`. Para gerar a versão final: `npm run build` (sai na pasta `dist/`).

**Opção 2: Live Server (sem instalar nada)**

No VS Code, instale a extensão **Live Server**, clique com o botão direito no `index.html` e escolha **Open with Live Server**.

> Abrir o `index.html` com dois cliques **não funciona**: o navegador bloqueia módulos JavaScript fora de um servidor.

### Contas de demonstração

| Perfil | Login | Senha |
|---|---|---|
| Secretaria (E.E. José Roque) | `conta.sec1` | `Escola@2026` |
| Secretaria (outra escola) | `conta.sec2` | `Escola@2026` |
| Administrador (Equipe Nitrix) | `admin.nitrix` | `Nitrix@2026` |

A tela de login tem botões que preenchem essas contas. A faixa amarela no topo tem
**Restaurar dados de exemplo**, que volta tudo ao estado inicial.

---

## Estrutura de pastas

```
src/
├── main.js                 Ponto de entrada: registra rotas e inicia o app
├── app/                    Estrutura geral
│   ├── router.js           Rotas por hash (#/alunos) e bloqueio por perfil
│   ├── layout.js           Menu lateral, topo do celular, faixa de demonstração
│   ├── session.js          Usuário logado e checagem de permissão
│   ├── eventos.js          Cliques (data-acao), formulários (data-form) e filtros (data-ui)
│   ├── acoes-gerais.js     Ações comuns: fechar modal, trocar aba, sair, exportar
│   ├── estado.js           Estado da interface (filtros, aba escolhida)
│   └── constantes.js       Valores do domínio (75%, 24 h, tipos, turnos...)
├── modules/                Um por domínio do sistema
│   ├── acesso/             Login, recuperar senha, telas 403/404
│   ├── inicio/             Painel da secretaria e do administrador
│   ├── frequencia/         Chamada diária, cancelamento, "chegou atrasado", cálculo
│   ├── turmas/             Turmas e alunos de cada turma
│   ├── alunos/             Lista, perfil, responsáveis e telefones, matrículas
│   ├── alerta/             Alerta de evasão e padrões de falta
│   ├── notificacao/        Fila de mensagens aos responsáveis
│   ├── busca-ativa/        Casos, tentativas de contato, ações, encerramento
│   ├── configuracao/       Turnos e envio: horário das mensagens e grade de horários
│   └── admin/              Escolas e usuários
├── components/             Peças reutilizáveis (modal, aviso, tabela, campos...)
├── data/
│   ├── api.js              ÚNICO ponto de acesso aos dados
│   └── mock/               Banco de demonstração no navegador + dados de exemplo
├── utils/                  Datas, textos, formatos, validação, listas
├── styles/                 tokens.css (cores e fontes), base, layout, components
└── assets/                 Ícone oficial (mochilinha)
```

### Regra de cada módulo

Cada pasta em `modules/` tem dois tipos de arquivo:

- **`*.service.js`**: regras de negócio. Não tem HTML. Valida os dados, checa permissão e chama `data/api.js`.
- **`*.page.js`**: telas. Monta o HTML, registra as ações dos botões e dos formulários e chama o service.

As telas **nunca** acessam os dados diretamente. O caminho é sempre:
`page → service → data/api.js → (hoje) mock | (depois) back-end`.

---

## Como ligar o back-end

Tudo passa por `src/data/api.js`. Cada coleção tem `listar`, `obter`, `criar`, `atualizar` e `remover`,
o equivalente a um endpoint REST. Para usar a API real, troque o corpo dessas funções por `fetch`:

```js
// antes (mock)
listar: async (filtros = {}) => store.listar(nome, filtros),

// depois (API real)
listar: async (filtros = {}) => (await fetch(`/api/${nome}?${new URLSearchParams(filtros)}`)).json(),
```

Como as coleções do mock correspondem às tabelas do banco:

| Coleção do mock | Tabela no banco |
|---|---|
| `escolas` (com `modeloTurno` e `horarioEnvio`) | `ESCOLA` (`modelo_turno`, `horario_envio_manha/tarde/noite/integral`) |
| `horarios` (com `vigencia`) | `HORARIO_AULA` (com `data_vigencia`) |
| `responsaveis`, `telefones` (`ordem` 0 = Principal) | `RESPONSAVEL`, `CONTATO_RESPONSAVEL` (`posicao` 1 = Principal) |
| `lancamentos` (com `horaEntrada`, `horaSaida`, `observacao`) | `REGISTRO_FREQUENCIA` |
| `notificacoes` (com a lista de tentativas) | `NOTIFICACAO` e `ENVIO_NOTIFICACAO` |
| `casos`, `acompanhamentos` | `CASO_BUSCA_ATIVA`, `ACOMPANHAMENTO_CASO` |

Pontos que **precisam ir para o servidor** quando o back-end existir:

| Hoje no navegador | No sistema real |
|---|---|
| `api.autenticar` e `data/mock/senha.js` | Login no servidor com hash bcrypt ou argon2 |
| `notificacao.service.js → processarFila()` (roda a cada 30 s) | Rotina agendada no servidor, a cada minuto |
| `fila.js → simularEntrega()` | Envio e recebimento pela API do WhatsApp |
| Checagens de perfil nos services | Também validar no servidor (o front pode ser burlado) |
| Exportar PDF pela impressão do navegador | Pode continuar assim ou ser gerado no servidor |

Os botões **Verificar agora**, **Avançar 24 h** e **Simular resposta** (tela Notificações) existem só
para a demonstração e devem ser removidos quando o servidor fizer o envio de verdade.

---

## Regras implementadas

**Acesso**
- Login e senha obrigatórios; erro genérico; 5 tentativas erradas bloqueiam por 15 minutos.
- Perfis: **Administrador** (Equipe Nitrix, todas as escolas) e **Secretaria** (uma escola).
- Endereço fora do perfil mostra "Acesso negado".

**Cadastros (somente administrador)**
- Escola (CIE único, 6 números), usuário da secretaria (login único, sempre com escola),
  turma por ano letivo, aluno e matrícula.
- No cadastro da escola: **modelo de turno** (Integral, ou Manhã, tarde e noite) e, opcional, o horário
  de envio de cada turno do modelo. O modelo só muda se as turmas do ano já estiverem no turno do novo modelo.
- A turma só aceita turnos do modelo da escola.
- A secretaria **consulta** turmas e **edita** os dados do aluno e dos responsáveis.

**Turnos e envio (administrador e secretaria da escola)**
- Para cada turno do modelo da escola: o **horário de envio** das mensagens de falta e a
  **grade de horários**: início e término de cada aula, em cada dia da semana. A quantidade de
  aulas do dia é o número de aulas cadastradas. Intervalo, almoço e tutoria não entram.
- Botão "Copiar de" para repetir a grade de um dia em outro.
- Turno sem horário de envio não manda mensagens. Turno sem grade não permite lançar ocorrências
  nem calcular a frequência das turmas dele; as telas avisam e levam para a configuração.
- **A grade tem data de vigência.** A primeira vale desde o início do ano letivo. Uma mudança vale
  a partir da data escolhida (hoje ou depois; hoje só se ainda não houver ocorrência lançada no
  turno). Os dias anteriores continuam com a grade que valia neles, então a frequência já calculada
  **nunca muda para trás**. Mudança que ainda não começou pode ser cancelada.
- Turno com grade não volta a ficar sem ela.

**Responsáveis e telefones**
- Todo aluno tem pelo menos um telefone de responsável (nunca do próprio aluno).
- Ordem de contato: **Principal**, **Secundário**, **Secundário 2**, **Secundário 3**... (setas para reordenar).
- Não é possível remover o último telefone.

**Frequência**
- A secretaria registra só quem faltou, chegou atrasado ou saiu. Atraso e saída pedem o horário
  e aceitam um motivo opcional.
- **Saída em dois passos:** na hora em que o aluno sai, registra só a hora da saída. Se ele voltar
  (por exemplo, de uma consulta), clica em **Voltou** ao lado do registro e informa a hora da volta:
  a saída vira "saiu e voltou" e as aulas depois da volta deixam de contar como perdidas.
- O sistema calcula as **aulas perdidas** pela grade em vigor na data, com a regra da chamada:
  a aula conta como presente se o aluno estava na escola no início dela.
  - Falta: todas as aulas do dia.
  - Atraso: as aulas que começaram antes da chegada.
  - Saída antecipada: as aulas que começam a partir da saída.
  - Saiu e voltou: as aulas que começam enquanto ele está fora.
- Antes de salvar, a tela mostra quais aulas serão perdidas. Atraso ou saída que perde todas as
  aulas é recusado: nesse caso é falta.
- Falta exclui outras ocorrências no dia; atraso, no máximo um; saída sem volta, no máximo uma
  em aberto. Os horários não podem se sobrepor.
- **Chegou atrasado**: troca a falta de hoje por atraso, pedindo a hora de chegada. Antes do
  horário de envio do turno, a mensagem nem sai.
- Só a falta dispara mensagem ao responsável.
- Registro nunca é apagado: é cancelado com motivo e continua no histórico.

**Cálculo e alerta**
- Frequência = (aulas previstas até hoje − aulas perdidas) ÷ aulas previstas.
  Aulas previstas = soma das aulas de cada dia de segunda a sexta desde o início da matrícula.
  Aulas perdidas = faltas, atrasos e saídas, cada aula contada uma vez por dia.
- Abaixo de **80%** o aluno entra no **Alerta de evasão** como "Atenção"; abaixo de **75%**
  (LDB, art. 24, VI), como "Crítico". A entrada é automática.
- Cada dia usa a grade de horários em vigor naquele dia.
- O alerta nunca abre caso de Busca Ativa sozinho.

**Padrões de falta (últimos 30 dias)**
- 3 ou mais faltas no mesmo dia da semana.
- O limite de 3 em 30 dias é uma referência proposta, **a validar com a equipe**.

**Notificação**
- No **horário de envio do turno** da turma do aluno, uma mensagem por aluno com falta no dia, para o telefone Principal.
- Sem resposta em 24 h, vai para o próximo telefone da lista, até o último; depois, "encerrada sem resposta".
- Envio que falha passa na hora para o próximo telefone e não trava as demais mensagens.
- Uma resposta interrompe a fila. Se a falta for cancelada, as próximas tentativas também param.

**Busca Ativa (somente secretaria)**
- Abertura manual, um caso aberto por aluno, tentativas de contato (canal, data e hora, responsável
  contatado, resultado, resumo), ações e encaminhamentos (tipo, data e hora, descrição), encerramento
  com data e desfecho. Caso encerrado é só consulta. O caso exporta em planilha e PDF.
- Na tela, tentativa e ação têm formulários separados; nos dados, ficam na mesma coleção
  (`acompanhamentos`, tabela `ACOMPANHAMENTO_CASO`), separadas pelo tipo do registro.

**Relatórios**
- Alerta de evasão e histórico do aluno exportam em planilha (.csv, abre no Excel) e em PDF,
  sempre com os filtros aplicados na tela.

---

## Pendências e decisões em aberto

- **Feriados e calendário escolar**: ficou para implementação futura. Hoje todo dia de segunda a sexta com aula no turno conta como letivo.
- **Limite dos padrões de falta**: validar os valores com a equipe.
- **Documento de requisitos**: atualizar para a configuração por turno (horário de envio e aulas por dia) no lugar da grade horária, disciplinas e 3ª aula.

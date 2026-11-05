// Ponto de entrada do EduAcompanha.
// Liga os dados, registra as rotas de cada módulo e inicia a navegação.

import { api } from './data/api.js';
import { registrarRotas, registrarTelasPublicas, registrarTelasErro, iniciarRoteador } from './app/router.js';
import { iniciarEventos } from './app/eventos.js';
import './app/acoes-gerais.js';
import { restaurarSessao } from './modules/acesso/acesso.service.js';

import * as acesso from './modules/acesso/acesso.page.js';
import * as inicio from './modules/inicio/inicio.page.js';
import * as frequencia from './modules/frequencia/frequencia.page.js';
import * as turmas from './modules/turmas/turmas.page.js';
import * as alunos from './modules/alunos/alunos.page.js';
import * as alerta from './modules/alerta/alerta.page.js';
import * as notificacao from './modules/notificacao/notificacao.page.js';
import * as buscaAtiva from './modules/busca-ativa/busca-ativa.page.js';
import * as escolas from './modules/admin/escolas.page.js';
import * as usuarios from './modules/admin/usuarios.page.js';
import * as turnos from './modules/configuracao/configuracao.page.js';

/** Intervalo da rotina de envio na demonstração. No sistema real ela roda no servidor. */
const INTERVALO_ROTINA_MS = 30_000;

async function iniciar() {
  api.iniciar();

  registrarTelasPublicas(acesso.telasPublicas);
  registrarTelasErro(acesso.telasErro);
  for (const modulo of [inicio, frequencia, turmas, alunos, alerta, notificacao, buscaAtiva, escolas, usuarios, turnos]) {
    registrarRotas(modulo.rotas);
  }

  iniciarEventos();
  await restaurarSessao();
  await notificacao.rodarRotina({ avisar: false });
  iniciarRoteador();

  setInterval(() => notificacao.rodarRotina({ avisar: true }), INTERVALO_ROTINA_MS);
}

iniciar();

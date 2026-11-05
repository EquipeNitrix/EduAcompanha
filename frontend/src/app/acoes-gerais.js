// Ações usadas em várias telas: fechar modal, trocar aba, sair, exportar...

import { registrarAcoes } from './eventos.js';
import { atualizar, navegar } from './router.js';
import { estado, limparEstado } from './estado.js';
import { alternarMenu, fecharMenu } from './layout.js';
import { alternarTema } from './tema.js';
import { api } from '../data/api.js';
import { fecharModal } from '../components/modal.js';
import { confirmar, executarConfirmacao } from '../components/confirm.js';
import { exportarCSVAtual, exportarPDFAtual } from '../components/export.js';
import { toast } from '../components/toast.js';
import { sair, restaurarSessao } from '../modules/acesso/acesso.service.js';
import { processarFila } from '../modules/notificacao/notificacao.service.js';

registrarAcoes({
  'modal:fechar': () => fecharModal(),
  'modal:confirmar': (_, botao) => executarConfirmacao(botao),

  'abas:selecionar': (d) => { estado('abas')[d.chave] = d.valor; atualizar(); },
  'lista:mais': (d) => {
    const mais = estado('mais');
    mais[d.chave] = (Number(mais[d.chave]) || Number(d.inicial)) + Number(d.passo || 50);
    atualizar();
  },
  'filtros:limpar': (d) => {
    const filtros = estado(d.chave);
    for (const campo of Object.keys(filtros)) filtros[campo] = '';
    atualizar();
  },

  'senha:alternar': (d, botao) => {
    const campo = document.getElementById(d.campo);
    const mostrar = campo.type === 'password';
    campo.type = mostrar ? 'text' : 'password';
    botao.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
  },

  'menu:alternar': () => alternarMenu(),
  'menu:fechar': () => fecharMenu(),
  'tema:alternar': () => alternarTema(),

  'sessao:sair': () => {
    sair();
    limparEstado();
    toast('Você saiu do sistema.');
    navegar('login');
  },

  'demo:restaurar': () => confirmar({
    titulo: 'Restaurar dados de exemplo?',
    texto: '<p>Tudo o que foi cadastrado ou lançado neste navegador será apagado, e os dados de exemplo voltam ao estado inicial.</p>',
    botao: 'Restaurar',
    aoConfirmar: async () => {
      await api.restaurarExemplo();
      limparEstado();
      await restaurarSessao();
      await processarFila();
      atualizar();
      toast('Dados de exemplo restaurados.');
    },
  }),

  'exportar:csv': () => exportarCSVAtual(),
  'exportar:pdf': () => exportarPDFAtual(),
});

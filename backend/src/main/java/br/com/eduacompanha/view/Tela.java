package br.com.eduacompanha.view;

import javax.swing.JOptionPane;

import br.com.eduacompanha.service.ValidacaoException;

public class Tela {
    public static void mostrar(String texto) {
        JOptionPane.showMessageDialog(null, texto);
    }

    public static void erro(Exception excecao) {
        if (excecao instanceof ValidacaoException v) {
            // juntar as mensagens mapa
        } else {
            JOptionPane.showMessageDialog(null, excecao.getMessage(), "ERRO", JOptionPane.ERROR_MESSAGE);
        }
    }

}

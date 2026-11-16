package br.com.eduacompanha.view;

import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import br.com.eduacompanha.service.ValidacaoException;

public class Tela {
    private static final ImageIcon icon = new ImageIcon(Tela.class.getResource("/icon.png"));

    public static void mostrar(String texto) {
        JOptionPane.showMessageDialog(null, texto, "EduAcompanha", JOptionPane.INFORMATION_MESSAGE, icon);
    }

    public static void erro(Exception excecao) {
        if (excecao instanceof ValidacaoException v) {
            String texto = "";
            for (String mensagem : v.getCampos().values()) {
                texto += mensagem + "\n";
            }
            JOptionPane.showMessageDialog(null, texto, "ERRO", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, excecao.getMessage(), "ERRO", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static int escolher(String titulo, String... opcoes) {
        return JOptionPane.showOptionDialog(null, titulo, "EduAcompanha", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, icon, opcoes, opcoes[0]);
    }

    public static String pedirTexto(String rotulo) {
        String resposta = (String) JOptionPane.showInputDialog(null, rotulo, "EduAcompanha",
                JOptionPane.QUESTION_MESSAGE, icon, null, null);
        if (resposta == null) {
            return null;
        } else {
            return resposta.trim();
        }
    }

    public static String pedirTexto(String rotulo, String atual) {
        String resposta = (String) JOptionPane.showInputDialog(null, rotulo, "EduAcompanha",
                JOptionPane.QUESTION_MESSAGE, icon, null, atual);
        if (resposta == null) {
            return null;
        } else {
            return resposta.trim();
        }
    }

    public static LocalTime pedirHora(String rotulo) {
        while (true) {
            String resposta = pedirTexto(rotulo);
            if (resposta == null || resposta.equals("")) {
                return null;
            }
            try {
                return LocalTime.parse(resposta, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                mostrar("Use o formato HH:MM, por exemplo 09:40.");
            }
        }
    }

    public static LocalTime pedirHora(String rotulo, LocalTime atual) {
        while (true) {
            String resposta = pedirTexto(rotulo, (atual == null ? null : atual.toString()));
            if (resposta == null || resposta.equals("")) {
                return null;
            }
            try {
                return LocalTime.parse(resposta, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                mostrar("Use o formato HH:MM, por exemplo 09:40.");
            }
        }
    }

    public static void mostrarLista(String titulo, String texto) {
        JTextArea caixa = new JTextArea(texto);
        caixa.setEditable(false);
        caixa.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane moldura = new JScrollPane(caixa);
        moldura.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(null, moldura, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    public static <T> T escolherDaLista(String titulo, List<T> itens) {
        if (itens.isEmpty()) {
            mostrar("Nenhum item cadastrado.");
            return null;
        }
        return (T) JOptionPane.showInputDialog(null, titulo, "EduAcompanha", JOptionPane.QUESTION_MESSAGE, icon,
                itens.toArray(), itens.get(0));
    }
}

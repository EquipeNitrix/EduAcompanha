package br.com.eduacompanha.view;

import java.util.ArrayList;
import javax.swing.JOptionPane;
import br.com.eduacompanha.model.Escola;
import br.com.eduacompanha.model.enums.ModeloTurno;
import br.com.eduacompanha.service.EscolaService;
/* 
public class MenuEscola {
    private EscolaService service;

    public MenuEscola(EscolaService service) {
        this.service = service;
    }

    public void abrir() {
        String[] opcoes = { "Listar", "Cadastrar", "Excluir", "Sair" };
        int opcao = 0;
        while (opcao != 3) {   // 3 = Sair
            opcao = JOptionPane.showOptionDialog(null, "Escolas", "EduAcompanha",
                    0, JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);
            if (opcao == -1) {
                opcao = 3;     // fechou a janela no X: sai também
            }
            if (opcao == 0) listar();
            if (opcao == 1) cadastrar();
            if (opcao == 2) excluir();
        }
    }

    private void listar() {
        ArrayList<Escola> escolas = service.listar();   // <- nome do método do seu Service
        String texto = "";
        for (Escola e : escolas) {
            texto += e.getCie() + " - " + e.getNomeEscola() + " - " + e.getModeloTurno() + "\n";
        }
        JOptionPane.showMessageDialog(null, texto);
    }

    private void cadastrar() {
        try {
            Escola e = new Escola();
            e.setCie(JOptionPane.showInputDialog("CIE:"));
            e.setNomeEscola(JOptionPane.showInputDialog("Nome:"));
            e.setMunicipio(JOptionPane.showInputDialog("Município:"));
            String modelo = JOptionPane.showInputDialog("Modelo (PARCIAL ou INTEGRAL):");
            e.setModeloTurno(ModeloTurno.valueOf(modelo.toUpperCase()));

            service.cadastrar(e);   // <- nome do método do seu Service
            JOptionPane.showMessageDialog(null, "Escola cadastrada!");
        } catch (Exception erro) {
            JOptionPane.showMessageDialog(null, "Erro: " + erro.getMessage());
        }
    }

    private void excluir() {
        try {
            String cie = JOptionPane.showInputDialog("CIE da escola para excluir:");
            service.excluir(cie);   // <- nome do método do seu Service
            JOptionPane.showMessageDialog(null, "Escola excluída!");
        } catch (Exception erro) {
            JOptionPane.showMessageDialog(null, "Erro: " + erro.getMessage());
        }
    }
}
*/
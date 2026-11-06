package br.com.eduacompanha.model;

import java.time.LocalDateTime;
import br.com.eduacompanha.model.enums.Perfil;

public class Usuario {
    private int idUsuario;
    private String nome;
    private String login;
    private String senhaHash;
    private Perfil perfil;
    private boolean ativo;
    private int tentativasFalhas;
    private LocalDateTime bloqueadoAte;
    private String cie;

    public Usuario() {
    }

    public Usuario(String nome, String login, String senhaHash, Perfil perfil, boolean ativo, int tentativasFalhas,
            LocalDateTime bloqueadoAte, String cie) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
        this.tentativasFalhas = tentativasFalhas;
        this.bloqueadoAte = bloqueadoAte;
        this.cie = cie;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(int tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(LocalDateTime bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public String getCie() {
        return cie;
    }

    public void setCie(String cie) {
        this.cie = cie;
    }

}

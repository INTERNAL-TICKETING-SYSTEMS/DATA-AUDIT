package com.fasec.auditoria.dto;

public class RegisterRequest {
    private String nome;
    private String email;
    private String emailPessoal;
    private String cpf;
    private String dataNascimento;
    private String genero;
    private String senha;
    private String telefoneWhatsapp;
    private String departamentoId;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmailPessoal() { return emailPessoal; }
    public void setEmailPessoal(String emailPessoal) { this.emailPessoal = emailPessoal; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getTelefoneWhatsapp() { return telefoneWhatsapp; }
    public void setTelefoneWhatsapp(String telefoneWhatsapp) { this.telefoneWhatsapp = telefoneWhatsapp; }

    public String getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(String departamentoId) { this.departamentoId = departamentoId; }
}
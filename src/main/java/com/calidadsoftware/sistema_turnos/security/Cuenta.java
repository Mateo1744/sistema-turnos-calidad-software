package com.calidadsoftware.sistema_turnos.security;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.calidadsoftware.sistema_turnos.entity.*;

@Entity
@Table(name = "cuentas")
public class Cuenta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Rol rol;
    @OneToOne @JoinColumn(unique = true)
    private Usuario usuario;
    @OneToOne @JoinColumn(unique = true)
    private Funcionario funcionario;

    protected Cuenta() {}
    public Cuenta(String username, String passwordHash, Rol rol, Usuario usuario, Funcionario funcionario) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.usuario = usuario;
        this.funcionario = funcionario;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    void cambiarPasswordHash(String hash) { this.passwordHash = hash; }
    public Rol getRol() { return rol; }
    public Usuario getUsuario() { return usuario; }
    public Funcionario getFuncionario() { return funcionario; }
}

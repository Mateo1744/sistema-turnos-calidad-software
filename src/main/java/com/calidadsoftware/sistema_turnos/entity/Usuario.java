package com.calidadsoftware.sistema_turnos.entity;

import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;

public class Usuario {

    private Long id;
    private String nombre;
    private String documento;
    private TipoUsuario tipoUsuario;

    public Usuario() {
    }

    public Usuario(Long id, String nombre, String documento, TipoUsuario tipoUsuario) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.tipoUsuario = tipoUsuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }
}
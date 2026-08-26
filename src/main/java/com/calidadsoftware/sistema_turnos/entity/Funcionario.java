package com.calidadsoftware.sistema_turnos.entity;

public class Funcionario {

    private Long id;
    private String nombre;
    private String estado;
    private Servicio servicio;

    public Funcionario() {
    }

    public Funcionario(Long id, String nombre, String estado, Servicio servicio) {
        this.id = id;
        this.nombre = nombre;
        this.estado = estado;
        this.servicio = servicio;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }
}
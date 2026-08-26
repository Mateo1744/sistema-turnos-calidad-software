package com.calidadsoftware.sistema_turnos.entity;

import java.time.LocalDateTime;

import com.calidadsoftware.sistema_turnos.enums.EstadoTurno;

public class Turno {

    private Long id;
    private String numero;
    private Usuario usuario;
    private Servicio servicio;
    private Funcionario funcionario;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinalizacion;
    private EstadoTurno estado;

    public Turno() {
    }

    public Turno(
            Long id,
            String numero,
            Usuario usuario,
            Servicio servicio,
            Funcionario funcionario,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFinalizacion,
            EstadoTurno estado) {

        this.id = id;
        this.numero = numero;
        this.usuario = usuario;
        this.servicio = servicio;
        this.funcionario = funcionario;
        this.fechaCreacion = fechaCreacion;
        this.fechaInicio = fechaInicio;
        this.fechaFinalizacion = fechaFinalizacion;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDateTime fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurno estado) {
        this.estado = estado;
    }
}
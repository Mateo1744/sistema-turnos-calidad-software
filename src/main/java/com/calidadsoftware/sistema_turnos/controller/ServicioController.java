package com.calidadsoftware.sistema_turnos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.calidadsoftware.sistema_turnos.entity.Servicio;
import com.calidadsoftware.sistema_turnos.service.ServicioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<Servicio> registrar(@Valid @RequestBody Servicio servicio) {
        Servicio registrado = servicioService.registrar(servicio);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrado);
    }

    @GetMapping
    public List<Servicio> listar() {
        return servicioService.listar();
    }
}

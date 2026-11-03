package com.calidadsoftware.sistema_turnos.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.service.*;

@RestController
public class ConsultasController {
    private final UsuarioService usuarios;
    private final ServicioService servicios;

    public ConsultasController(UsuarioService usuarios, ServicioService servicios) {
        this.usuarios = usuarios;
        this.servicios = servicios;
    }

    @GetMapping("/usuarios")
    public List<Usuario> usuarios() { return usuarios.listar(); }

    @GetMapping("/usuarios/{id}")
    public Usuario usuario(@PathVariable Long id) { return usuarios.buscarPorId(id); }

    @GetMapping("/servicios/{id}")
    public Servicio servicio(@PathVariable Long id) { return servicios.buscarPorId(id); }
}

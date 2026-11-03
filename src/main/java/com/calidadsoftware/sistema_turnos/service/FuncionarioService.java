package com.calidadsoftware.sistema_turnos.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.Funcionario;

@Service
public class FuncionarioService {
    private final ServicioService servicios;
    private final com.calidadsoftware.sistema_turnos.repository.FuncionarioRepository funcionarios;

    public FuncionarioService(ServicioService servicios,
            com.calidadsoftware.sistema_turnos.repository.FuncionarioRepository funcionarios) {
        this.servicios = servicios;
        this.funcionarios = funcionarios;
    }

    public synchronized Funcionario registrar(String nombre, Long servicioId) {
        var servicio = servicios.buscarPorId(servicioId);
        if (!"ACTIVO".equals(servicio.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio no está activo");
        }
        var funcionario = new Funcionario(null, nombre.trim(), "ACTIVO", servicio);
        return funcionarios.saveAndFlush(funcionario);
    }

    public synchronized Funcionario buscarPorId(Long id) {
        return funcionarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario no encontrado"));
    }

    public synchronized List<Funcionario> listar() {
        return funcionarios.findAll(org.springframework.data.domain.Sort.by("id"));
    }
}

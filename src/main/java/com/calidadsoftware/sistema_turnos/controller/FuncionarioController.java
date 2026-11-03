package com.calidadsoftware.sistema_turnos.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.calidadsoftware.sistema_turnos.dto.CrearFuncionarioRequest;
import com.calidadsoftware.sistema_turnos.entity.Funcionario;
import com.calidadsoftware.sistema_turnos.service.FuncionarioService;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {
    private final FuncionarioService funcionarios;
    private final com.calidadsoftware.sistema_turnos.security.AccesoTurnos acceso;
    public FuncionarioController(FuncionarioService funcionarios,
            com.calidadsoftware.sistema_turnos.security.AccesoTurnos acceso) {
        this.funcionarios = funcionarios; this.acceso = acceso;
    }

    @PostMapping
    public ResponseEntity<Funcionario> registrar(@Valid @RequestBody CrearFuncionarioRequest solicitud) {
        var funcionario = funcionarios.registrar(solicitud.nombre(), solicitud.servicioId());
        return ResponseEntity.created(URI.create("/funcionarios/" + funcionario.getId())).body(funcionario);
    }

    @GetMapping
    public List<Funcionario> listar() {
        return acceso.admin() ? funcionarios.listar() : List.of(acceso.funcionario());
    }

    @GetMapping("/{id}")
    public Funcionario buscar(@PathVariable Long id) {
        acceso.verificarFuncionario(id); return funcionarios.buscarPorId(id);
    }
}

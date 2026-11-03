package com.calidadsoftware.sistema_turnos.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.calidadsoftware.sistema_turnos.dto.CrearTurnoRequest;
import com.calidadsoftware.sistema_turnos.entity.Turno;
import com.calidadsoftware.sistema_turnos.service.TurnoService;

@RestController
@RequestMapping("/turnos")
public class TurnoController {
    private final TurnoService turnos;
    private final com.calidadsoftware.sistema_turnos.security.AccesoTurnos acceso;

    @PatchMapping("/{id}/esperar")
    public Turno esperar(@PathVariable Long id) { acceso.verificar(id, false); return turnos.ponerEnEspera(id); }

    @PatchMapping("/{id}/llamar")
    public Turno llamar(@PathVariable Long id,
            @Valid @RequestBody com.calidadsoftware.sistema_turnos.dto.LlamarTurnoRequest solicitud) {
        acceso.verificar(id, false);
        acceso.verificarFuncionario(solicitud.funcionarioId());
        return turnos.llamar(id, solicitud.funcionarioId());
    }

    @PatchMapping("/{id}/iniciar")
    public Turno iniciar(@PathVariable Long id) { acceso.verificar(id, true); return turnos.iniciar(id); }

    @PatchMapping("/{id}/finalizar")
    public Turno finalizar(@PathVariable Long id) { acceso.verificar(id, true); return turnos.finalizar(id); }

    @PatchMapping("/{id}/cancelar")
    public Turno cancelar(@PathVariable Long id) { acceso.verificar(id, false); return turnos.cancelar(id); }

    @GetMapping("/funcionario/{id}")
    public List<Turno> porFuncionario(@PathVariable Long id) {
        acceso.verificarFuncionario(id); return turnos.porFuncionario(id);
    }

    public TurnoController(TurnoService turnos, com.calidadsoftware.sistema_turnos.security.AccesoTurnos acceso) {
        this.turnos = turnos;
        this.acceso = acceso;
    }

    @PostMapping
    public ResponseEntity<Turno> crear(@Valid @RequestBody CrearTurnoRequest solicitud) {
        Turno turno = turnos.crear(solicitud.usuarioId(), solicitud.servicioId());
        return ResponseEntity.created(URI.create("/turnos/" + turno.getId())).body(turno);
    }

    @GetMapping
    public List<Turno> listar(@RequestParam(required = false) Long servicioId) {
        return turnos.listar(servicioId).stream().filter(acceso::visible).toList();
    }

    @GetMapping("/{id}")
    public Turno buscarPorId(@PathVariable Long id) {
        acceso.verificar(id, false);
        return turnos.buscarPorId(id);
    }

    @GetMapping("/numero/{numero}")
    public Turno buscarPorNumero(@PathVariable String numero) {
        var turno = turnos.buscarPorNumero(numero);
        acceso.verificar(turno.getId(), false);
        return turno;
    }

    @GetMapping("/pendientes")
    public List<Turno> pendientes() {
        return turnos.pendientes().stream().filter(acceso::visible).toList();
    }
}

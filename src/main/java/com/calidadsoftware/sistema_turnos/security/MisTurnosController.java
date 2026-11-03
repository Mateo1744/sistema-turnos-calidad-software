package com.calidadsoftware.sistema_turnos.security;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import com.calidadsoftware.sistema_turnos.entity.Turno;
import com.calidadsoftware.sistema_turnos.enums.EstadoTurno;
import com.calidadsoftware.sistema_turnos.service.TurnoService;

@RestController
@RequestMapping("/api/mi/turnos")
public class MisTurnosController {
    private final TurnoService turnos;
    public MisTurnosController(TurnoService turnos) { this.turnos = turnos; }
    public record Solicitud(@NotNull @Positive Long servicioId) {}

    @GetMapping
    public List<Turno> listar(@AuthenticationPrincipal CuentaPrincipal cuenta) {
        return turnos.listar(null).stream().filter(t -> t.getUsuario().getId().equals(cuenta.usuarioId())).toList();
    }
    @PostMapping
    @Transactional
    public ResponseEntity<Turno> crear(@AuthenticationPrincipal CuentaPrincipal cuenta,
            @Valid @RequestBody Solicitud solicitud) {
        var turno = turnos.crear(cuenta.usuarioId(), solicitud.servicioId());
        turnos.ponerEnEspera(turno.getId());
        return ResponseEntity.status(201).body(turno);
    }
    @PatchMapping("/{id}/cancelar")
    public Turno cancelar(@AuthenticationPrincipal CuentaPrincipal cuenta, @PathVariable Long id) {
        var turno = turnos.buscarPorId(id);
        if (!turno.getUsuario().getId().equals(cuenta.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
        if (turno.getEstado() != EstadoTurno.GENERADO && turno.getEstado() != EstadoTurno.EN_ESPERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes cancelar turnos que aún no han sido llamados");
        }
        return turnos.cancelar(id);
    }
}

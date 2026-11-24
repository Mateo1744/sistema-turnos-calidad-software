package com.calidadsoftware.sistema_turnos.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.enums.*;
import com.calidadsoftware.sistema_turnos.service.TurnoService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MisTurnosControllerTest {
    private final TurnoService turnos = mock(TurnoService.class);
    private final MisTurnosController api = new MisTurnosController(turnos);
    private final CuentaPrincipal cliente = new CuentaPrincipal(1L, "ana", "hash", Rol.USUARIO, 3L, null);

    private Turno turnoPropio(EstadoTurno estado) {
        var turno = new Turno();
        turno.setUsuario(new Usuario(3L, "Ana", "123", TipoUsuario.NORMAL));
        turno.setEstado(estado);
        when(turnos.buscarPorId(9L)).thenReturn(turno);
        return turno;
    }

    @Test
    void listadoDevuelveUnicamenteLosTurnosPropios() {
        var propio = turnoPropio(EstadoTurno.GENERADO);
        var ajeno = new Turno();
        ajeno.setUsuario(new Usuario(5L, "Luis", "456", TipoUsuario.NORMAL));
        when(turnos.listar(null)).thenReturn(List.of(ajeno, propio));
        assertEquals(List.of(propio), api.listar(cliente));
    }

    @Test
    void creacionDevuelveElTurnoYLoPoneEnEspera() {
        var propio = turnoPropio(EstadoTurno.GENERADO);
        propio.setId(9L);
        when(turnos.crear(3L, 4L)).thenReturn(propio);
        var respuesta = api.crear(cliente, new MisTurnosController.Solicitud(4L));
        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(propio, respuesta.getBody());
        verify(turnos).ponerEnEspera(9L);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"GENERADO", "EN_ESPERA"})
    void permiteCancelarAntesDelLlamado(EstadoTurno estado) {
        var turno = turnoPropio(estado);
        when(turnos.cancelar(9L)).thenReturn(turno);
        assertSame(turno, api.cancelar(cliente, 9L));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"GENERADO", "EN_ESPERA"}, mode = EnumSource.Mode.EXCLUDE)
    void rechazaCancelarDesdeOtrosEstados(EstadoTurno estado) {
        turnoPropio(estado);
        var error = assertThrows(ResponseStatusException.class, () -> api.cancelar(cliente, 9L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(turnos, never()).cancelar(9L);
    }
}

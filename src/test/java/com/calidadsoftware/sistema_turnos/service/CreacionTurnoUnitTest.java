package com.calidadsoftware.sistema_turnos.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.Servicio;
import com.calidadsoftware.sistema_turnos.entity.Turno;
import com.calidadsoftware.sistema_turnos.entity.Usuario;
import com.calidadsoftware.sistema_turnos.enums.EstadoTurno;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;
import com.calidadsoftware.sistema_turnos.repository.TurnoRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreacionTurnoUnitTest {
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final ServicioService servicios = mock(ServicioService.class);
    private final TurnoRepository repositorio = mock(TurnoRepository.class);
    private final TurnoService turnos = new TurnoService(usuarios, servicios,
            mock(FuncionarioService.class), repositorio);
    private final Usuario ana = new Usuario(3L, "Ana", "123", TipoUsuario.NORMAL);
    private final Servicio pagos = new Servicio(1L, "Pagos", "Caja", "ACTIVO");

    @BeforeEach
    void prepararSolicitud() {
        when(usuarios.buscarPorId(3L)).thenReturn(ana);
        when(servicios.buscarPorId(1L)).thenReturn(pagos);
    }

    private void simularIdGenerado() {
        when(repositorio.saveAndFlush(any(Turno.class))).thenAnswer(invocacion -> {
            Turno nuevo = invocacion.getArgument(0);
            nuevo.setId(42L);
            return nuevo;
        });
    }

    private Turno turnoPrevio(Servicio area, EstadoTurno estado) {
        var previo = new Turno();
        previo.setUsuario(ana);
        previo.setServicio(area);
        previo.setEstado(estado);
        return previo;
    }

    @Test
    void creaTurnoNumeradoConUsuarioServicioYEstadoInicial() {
        simularIdGenerado();
        var nuevo = turnos.crear(3L, 1L);
        assertEquals("T042", nuevo.getNumero());
        assertEquals(EstadoTurno.GENERADO, nuevo.getEstado());
        assertSame(ana, nuevo.getUsuario());
        assertSame(pagos, nuevo.getServicio());
        assertNotNull(nuevo.getFechaCreacion());
        assertNull(nuevo.getFuncionario());
    }

    @Test
    void rechazaServicioInactivoSinGuardar() {
        pagos.setEstado("INACTIVO");
        var error = assertThrows(ResponseStatusException.class, () -> turnos.crear(3L, 1L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void rechazaSegundoTurnoActivoDelMismoUsuarioYServicio() {
        when(repositorio.findAll()).thenReturn(List.of(turnoPrevio(pagos, EstadoTurno.EN_ATENCION)));
        var error = assertThrows(ResponseStatusException.class, () -> turnos.crear(3L, 1L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void permiteSolicitarDeNuevoTrasUnaCancelacion() {
        when(repositorio.findAll()).thenReturn(List.of(turnoPrevio(pagos, EstadoTurno.CANCELADO)));
        simularIdGenerado();
        assertEquals(EstadoTurno.GENERADO, turnos.crear(3L, 1L).getEstado());
    }

    @Test
    void permiteTurnoActivoEnOtroServicio() {
        var soporte = new Servicio(2L, "Soporte", "Ayuda", "ACTIVO");
        when(repositorio.findAll()).thenReturn(List.of(turnoPrevio(soporte, EstadoTurno.EN_ESPERA)));
        simularIdGenerado();
        assertSame(pagos, turnos.crear(3L, 1L).getServicio());
    }
}

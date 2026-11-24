package com.calidadsoftware.sistema_turnos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.enums.*;
import com.calidadsoftware.sistema_turnos.repository.TurnoRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TurnoServiceUnitTest {
    private final TurnoRepository repositorio = mock(TurnoRepository.class);
    private final FuncionarioService funcionarios = mock(FuncionarioService.class);
    private final TurnoService servicio = new TurnoService(mock(UsuarioService.class),
            mock(ServicioService.class), funcionarios, repositorio);
    private final Servicio pagos = new Servicio(1L, "Pagos", "Caja", "ACTIVO");
    private final Funcionario agente = new Funcionario(7L, "Ana", "ACTIVO", pagos);

    private Turno turno(long id, TipoUsuario tipo, EstadoTurno estado) {
        var usuario = new Usuario(id, "Usuario", Long.toString(id), tipo);
        var turno = new Turno(id, "T" + id, usuario, pagos, null, LocalDateTime.now(), null, null, estado);
        when(repositorio.findById(id)).thenReturn(Optional.of(turno));
        return turno;
    }

    @Test
    void buscaSoloElNumeroSolicitado() {
        var primero = turno(1, TipoUsuario.NORMAL, EstadoTurno.GENERADO);
        var segundo = turno(2, TipoUsuario.NORMAL, EstadoTurno.GENERADO);
        when(repositorio.findAll()).thenReturn(List.of(primero, segundo));

        assertSame(segundo, servicio.buscarPorNumero("t2"));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.buscarPorNumero("T99"));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void consultaSoloLosTurnosDelFuncionario() {
        var propio = turno(1, TipoUsuario.NORMAL, EstadoTurno.FINALIZADO);
        propio.setFuncionario(agente);
        var ajeno = turno(2, TipoUsuario.NORMAL, EstadoTurno.LLAMADO);
        ajeno.setFuncionario(new Funcionario(8L, "Laura", "ACTIVO", pagos));
        var libre = turno(3, TipoUsuario.NORMAL, EstadoTurno.EN_ESPERA);
        when(funcionarios.buscarPorId(7L)).thenReturn(agente);
        when(repositorio.findAll(Sort.by("id"))).thenReturn(List.of(ajeno, libre, propio));

        assertEquals(List.of(propio), servicio.porFuncionario(7L));
    }

    @Test
    void iniciaAtencionDeTurnoLlamado() {
        var turno = turno(1, TipoUsuario.NORMAL, EstadoTurno.LLAMADO);
        servicio.iniciar(1L);
        assertEquals(EstadoTurno.EN_ATENCION, turno.getEstado());
        assertNotNull(turno.getFechaInicio());
    }

    @Test
    void finalizaYRegistraFechaCoherente() {
        var turno = turno(1, TipoUsuario.NORMAL, EstadoTurno.EN_ATENCION);
        turno.setFechaInicio(LocalDateTime.now().minusMinutes(5));
        servicio.finalizar(1L);
        assertEquals(EstadoTurno.FINALIZADO, turno.getEstado());
        assertFalse(turno.getFechaFinalizacion().isBefore(turno.getFechaInicio()));
    }

    @Test
    void impideFinalizarAntesDeIniciar() {
        var turno = turno(1, TipoUsuario.NORMAL, EstadoTurno.EN_ATENCION);
        turno.setFechaInicio(LocalDateTime.MAX);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.finalizar(1L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoTurno.EN_ATENCION, turno.getEstado());
        assertNull(turno.getFechaFinalizacion());
    }

    @Test
    void rechazaFinalizarDesdeEspera() {
        var turno = turno(1, TipoUsuario.NORMAL, EstadoTurno.EN_ESPERA);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.finalizar(1L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoTurno.EN_ESPERA, turno.getEstado());
        assertNull(turno.getFechaFinalizacion());
    }

    @Test
    void rechazaIniciarUnTurnoCancelado() {
        var turno = turno(1, TipoUsuario.NORMAL, EstadoTurno.CANCELADO);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.iniciar(1L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        assertNull(turno.getFechaInicio());
    }

    @Test
    void cancelaUnTurnoGenerado() {
        var turno = turno(1, TipoUsuario.NORMAL, EstadoTurno.GENERADO);
        servicio.cancelar(1L);
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
    }

    @Test
    void exigePrioridadAunqueElNormalLlegoPrimero() {
        var normal = turno(1, TipoUsuario.NORMAL, EstadoTurno.EN_ESPERA);
        var prioritario = turno(2, TipoUsuario.PRIORITARIO, EstadoTurno.EN_ESPERA);
        when(funcionarios.buscarPorId(7L)).thenReturn(agente);
        when(repositorio.findAll()).thenReturn(List.of(normal, prioritario));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.llamar(1L, 7L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoTurno.EN_ESPERA, normal.getEstado());
        assertNull(normal.getFuncionario());
        servicio.llamar(2L, 7L);
        assertEquals(EstadoTurno.LLAMADO, prioritario.getEstado());
        assertSame(agente, prioritario.getFuncionario());
    }

    @Test
    void impideAsignarUnFuncionarioOcupado() {
        var actual = turno(1, TipoUsuario.NORMAL, EstadoTurno.EN_ATENCION);
        actual.setFuncionario(agente);
        var espera = turno(2, TipoUsuario.NORMAL, EstadoTurno.EN_ESPERA);
        when(funcionarios.buscarPorId(7L)).thenReturn(agente);
        when(repositorio.findAll()).thenReturn(List.of(actual, espera));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.llamar(2L, 7L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertNull(espera.getFuncionario());
        assertEquals(EstadoTurno.EN_ESPERA, espera.getEstado());
    }
}

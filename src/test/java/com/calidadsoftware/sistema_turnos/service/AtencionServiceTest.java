package com.calidadsoftware.sistema_turnos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.enums.*;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@org.springframework.transaction.annotation.Transactional
class AtencionServiceTest {
    @org.springframework.beans.factory.annotation.Autowired
    private UsuarioService usuarios;
    @org.springframework.beans.factory.annotation.Autowired
    private ServicioService servicios;
    @org.springframework.beans.factory.annotation.Autowired
    private FuncionarioService funcionarios;
    @org.springframework.beans.factory.annotation.Autowired
    private TurnoService turnos;

    @BeforeEach
    void preparar() {
        usuarios.registrar(new Usuario(null, "Ana", "1", TipoUsuario.NORMAL));
        usuarios.registrar(new Usuario(null, "Luis", "2", TipoUsuario.PRIORITARIO));
        servicios.registrar(new Servicio(null, "Pagos", "Pagos", "ACTIVO"));
        funcionarios.registrar("Laura", 1L);
    }

    @Test
    void completaCicloYPermiteNuevoTurno() {
        var t = turnos.crear(1L, 1L);
        var turnoId = t.getId();
        turnos.ponerEnEspera(turnoId);
        turnos.llamar(turnoId, 1L);
        assertEquals(EstadoTurno.LLAMADO, t.getEstado());
        turnos.iniciar(turnoId);
        assertNotNull(t.getFechaInicio());
        turnos.finalizar(turnoId);
        assertEquals(EstadoTurno.FINALIZADO, t.getEstado());
        assertFalse(t.getFechaFinalizacion().isBefore(t.getFechaInicio()));
        assertEquals(1, turnos.porFuncionario(1L).size());
        assertTrue(turnos.pendientes().isEmpty());
        assertNotEquals(t.getNumero(), turnos.crear(1L, 1L).getNumero());
        assertThrows(ResponseStatusException.class, () -> turnos.iniciar(turnoId));
        assertThrows(ResponseStatusException.class, () -> turnos.cancelar(turnoId));
    }

    @Test
    void respetaPrioridadYReservaFuncionario() {
        var normal = turnos.crear(1L, 1L);
        var normalId = normal.getId();
        var prioritario = turnos.crear(2L, 1L);
        var prioritarioId = prioritario.getId();
        turnos.ponerEnEspera(normalId);
        turnos.ponerEnEspera(prioritarioId);
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(normalId, 1L));
        turnos.llamar(prioritarioId, 1L);
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(normalId, 1L));
        turnos.iniciar(prioritarioId);
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(normalId, 1L));
        turnos.finalizar(prioritarioId);
        assertDoesNotThrow(() -> turnos.llamar(normalId, 1L));
    }

    @Test
    void rechazaSaltosYTurnosCancelados() {
        var t = turnos.crear(1L, 1L);
        var turnoId = t.getId();
        assertThrows(ResponseStatusException.class, () -> turnos.finalizar(turnoId));
        assertThrows(ResponseStatusException.class, () -> turnos.iniciar(turnoId));
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(turnoId, 1L));
        turnos.ponerEnEspera(turnoId);
        assertThrows(ResponseStatusException.class, () -> turnos.finalizar(turnoId));
        turnos.cancelar(turnoId);
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(turnoId, 1L));
        assertThrows(ResponseStatusException.class, () -> turnos.iniciar(turnoId));
        assertThrows(ResponseStatusException.class, () -> turnos.ponerEnEspera(turnoId));
        assertDoesNotThrow(() -> turnos.crear(1L, 1L));
    }

    @Test
    void rechazaFuncionarioDeOtroServicio() {
        servicios.registrar(new Servicio(null, "Soporte", "Ayuda", "ACTIVO"));
        funcionarios.registrar("Pedro", 2L);
        var t = turnos.crear(1L, 1L);
        var turnoId = t.getId();
        turnos.ponerEnEspera(turnoId);
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(turnoId, 2L));
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(turnoId, 99L));
        assertEquals(EstadoTurno.EN_ESPERA, t.getEstado());
    }
}

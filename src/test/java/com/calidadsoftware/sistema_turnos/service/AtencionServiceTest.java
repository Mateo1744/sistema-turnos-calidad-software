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
        turnos.ponerEnEspera(t.getId());
        turnos.llamar(t.getId(), 1L);
        assertEquals(EstadoTurno.LLAMADO, t.getEstado());
        turnos.iniciar(t.getId());
        assertNotNull(t.getFechaInicio());
        turnos.finalizar(t.getId());
        assertEquals(EstadoTurno.FINALIZADO, t.getEstado());
        assertFalse(t.getFechaFinalizacion().isBefore(t.getFechaInicio()));
        assertEquals(1, turnos.porFuncionario(1L).size());
        assertTrue(turnos.pendientes().isEmpty());
        assertNotEquals(t.getNumero(), turnos.crear(1L, 1L).getNumero());
        assertThrows(ResponseStatusException.class, () -> turnos.iniciar(t.getId()));
        assertThrows(ResponseStatusException.class, () -> turnos.cancelar(t.getId()));
    }

    @Test
    void respetaPrioridadYReservaFuncionario() {
        var normal = turnos.crear(1L, 1L);
        var prioritario = turnos.crear(2L, 1L);
        turnos.ponerEnEspera(normal.getId());
        turnos.ponerEnEspera(prioritario.getId());
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(normal.getId(), 1L));
        turnos.llamar(prioritario.getId(), 1L);
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(normal.getId(), 1L));
        turnos.iniciar(prioritario.getId());
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(normal.getId(), 1L));
        turnos.finalizar(prioritario.getId());
        assertDoesNotThrow(() -> turnos.llamar(normal.getId(), 1L));
    }

    @Test
    void rechazaSaltosYTurnosCancelados() {
        var t = turnos.crear(1L, 1L);
        assertThrows(ResponseStatusException.class, () -> turnos.finalizar(t.getId()));
        assertThrows(ResponseStatusException.class, () -> turnos.iniciar(t.getId()));
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(t.getId(), 1L));
        turnos.ponerEnEspera(t.getId());
        assertThrows(ResponseStatusException.class, () -> turnos.finalizar(t.getId()));
        turnos.cancelar(t.getId());
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(t.getId(), 1L));
        assertThrows(ResponseStatusException.class, () -> turnos.iniciar(t.getId()));
        assertThrows(ResponseStatusException.class, () -> turnos.ponerEnEspera(t.getId()));
        assertDoesNotThrow(() -> turnos.crear(1L, 1L));
    }

    @Test
    void rechazaFuncionarioDeOtroServicio() {
        servicios.registrar(new Servicio(null, "Soporte", "Ayuda", "ACTIVO"));
        funcionarios.registrar("Pedro", 2L);
        var t = turnos.crear(1L, 1L);
        turnos.ponerEnEspera(t.getId());
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(t.getId(), 2L));
        assertThrows(ResponseStatusException.class, () -> turnos.llamar(t.getId(), 99L));
        assertEquals(EstadoTurno.EN_ESPERA, t.getEstado());
    }
}

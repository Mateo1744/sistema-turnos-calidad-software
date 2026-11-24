package com.calidadsoftware.sistema_turnos.entity;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;
import static org.junit.jupiter.api.Assertions.*;

class DatosTurnoTest {
    @Test
    void conservaDatosDelFuncionarioAlAsignarloAlTurno() {
        var pagos = new Servicio(1L, "Pagos", "Caja", "ACTIVO");
        var funcionario = new Funcionario();
        funcionario.setId(7L);
        funcionario.setNombre("Ana");
        funcionario.setEstado("ACTIVO");
        funcionario.setServicio(pagos);
        var turno = new Turno();
        turno.setFuncionario(funcionario);
        var asignado = turno.getFuncionario();
        assertEquals(7L, asignado.getId());
        assertEquals("Ana", asignado.getNombre());
        assertEquals("ACTIVO", asignado.getEstado());
        assertSame(pagos, asignado.getServicio());
    }

    @Test
    void conservaFechaDeCreacionYPrioridadDelUsuario() {
        var fecha = LocalDateTime.of(2026, 3, 16, 8, 30);
        var turno = new Turno();
        var usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PRIORITARIO);
        turno.setUsuario(usuario);
        turno.setFechaCreacion(fecha);
        assertEquals(fecha, turno.getFechaCreacion());
        assertEquals(TipoUsuario.PRIORITARIO, turno.getUsuario().getTipoUsuario());
    }
}

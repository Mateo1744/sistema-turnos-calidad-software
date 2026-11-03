package com.calidadsoftware.sistema_turnos;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.enums.*;
import com.calidadsoftware.sistema_turnos.service.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenciaIntegrationTests {
    @Autowired private UsuarioService usuarios;
    @Autowired private ServicioService servicios;
    @Autowired private FuncionarioService funcionarios;
    @Autowired private TurnoService turnos;
    @Autowired private EntityManager entityManager;

    @Test
    void recuperaRelacionesYFechasDesdeLaBase() {
        var usuario = usuarios.registrar(new Usuario(null, "Persistencia", "PERSIST-01", TipoUsuario.NORMAL));
        var servicio = servicios.registrar(new Servicio(null, "Persistencia", "Prueba de guardado", "ACTIVO"));
        var funcionario = funcionarios.registrar("Funcionaria de prueba", servicio.getId());
        var turno = turnos.crear(usuario.getId(), servicio.getId());
        var numero = turno.getNumero();
        turnos.ponerEnEspera(turno.getId());
        turnos.llamar(turno.getId(), funcionario.getId());
        turnos.iniciar(turno.getId());
        turnos.finalizar(turno.getId());
        entityManager.flush();
        entityManager.clear();
        var recuperado = turnos.buscarPorNumero(numero);
        assertNotSame(turno, recuperado);
        assertEquals(EstadoTurno.FINALIZADO, recuperado.getEstado());
        assertEquals(usuario.getId(), recuperado.getUsuario().getId());
        assertEquals(servicio.getId(), recuperado.getServicio().getId());
        assertEquals(funcionario.getId(), recuperado.getFuncionario().getId());
        assertNotNull(recuperado.getFechaInicio());
        assertNotNull(recuperado.getFechaFinalizacion());
    }
}

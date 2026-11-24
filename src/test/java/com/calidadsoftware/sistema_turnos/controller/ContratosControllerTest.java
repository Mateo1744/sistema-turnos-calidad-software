package com.calidadsoftware.sistema_turnos.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.calidadsoftware.sistema_turnos.dto.LlamarTurnoRequest;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.security.AccesoTurnos;
import com.calidadsoftware.sistema_turnos.service.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContratosControllerTest {
    private final AccesoTurnos acceso = mock(AccesoTurnos.class);
    private final TurnoService turnos = mock(TurnoService.class);
    private final TurnoController api = new TurnoController(turnos, acceso);

    @Test
    void deniegaCadaTransicionDeUnTurnoFueraDeAlcance() {
        doThrow(AccessDeniedException.class).when(acceso).verificar(3L, false);
        doThrow(AccessDeniedException.class).when(acceso).verificar(3L, true);
        var solicitud = new LlamarTurnoRequest(1L);
        assertThrows(AccessDeniedException.class, () -> api.esperar(3L));
        assertThrows(AccessDeniedException.class, () -> api.llamar(3L, solicitud));
        assertThrows(AccessDeniedException.class, () -> api.cancelar(3L));
        assertThrows(AccessDeniedException.class, () -> api.finalizar(3L));
        verifyNoInteractions(turnos);
    }

    @Test
    void deniegaConsultaPorNumeroAunqueElTurnoExista() {
        var turno = new Turno();
        turno.setId(3L);
        when(turnos.buscarPorNumero("T003")).thenReturn(turno);
        doThrow(AccessDeniedException.class).when(acceso).verificar(3L, false);
        assertThrows(AccessDeniedException.class, () -> api.buscarPorNumero("T003"));
    }

    @Test
    void aplicaElPermisoAlPerfilYALaColaDelFuncionario() {
        var funcionarios = mock(FuncionarioService.class);
        var controlador = new FuncionarioController(funcionarios, acceso);
        doThrow(AccessDeniedException.class).when(acceso).verificarFuncionario(2L);
        assertThrows(AccessDeniedException.class, () -> controlador.buscar(2L));
        assertThrows(AccessDeniedException.class, () -> api.porFuncionario(2L));
        verifyNoInteractions(funcionarios, turnos);
        var autorizado = new Funcionario();
        when(funcionarios.buscarPorId(1L)).thenReturn(autorizado);
        assertSame(autorizado, controlador.buscar(1L));
    }

    @Test
    void consultasDevuelvenLaEntidadSolicitada() {
        var usuarios = mock(UsuarioService.class);
        var servicios = mock(ServicioService.class);
        var apiConsultas = new ConsultasController(usuarios, servicios);
        var usuario = new Usuario();
        var servicio = new Servicio();
        when(usuarios.buscarPorId(5L)).thenReturn(usuario);
        when(servicios.buscarPorId(4L)).thenReturn(servicio);
        assertSame(usuario, apiConsultas.usuario(5L));
        assertSame(servicio, apiConsultas.servicio(4L));
    }

    @Test
    void conflictoDeIntegridadDevuelveDetalleYEstado409() {
        var respuesta = new ApiExceptionHandler().conflicto();
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().getStatus());
        assertTrue(respuesta.getBody().getDetail().contains("duplicado"));
    }

    @Test
    void validacionIdentificaElCampoYLaCausa() {
        var binding = new BeanPropertyBindingResult(new Object(), "solicitud");
        binding.addError(new FieldError("solicitud", "nombre", "es obligatorio"));
        var error = mock(MethodArgumentNotValidException.class);
        when(error.getBindingResult()).thenReturn(binding);
        var respuesta = new ApiExceptionHandler().validacion(error);
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(List.of("nombre: es obligatorio"), respuesta.getBody().getProperties().get("errores"));
    }
}

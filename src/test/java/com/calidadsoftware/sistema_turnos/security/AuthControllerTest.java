package com.calidadsoftware.sistema_turnos.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    private final CuentaService cuentas = mock(CuentaService.class);
    private final AuthController api = new AuthController(cuentas);

    @Test
    void registroDeUsuarioEntregaDatosAlServicio() {
        var solicitud = new AuthController.Registro("ana", "clave-de-prueba", "Ana", "123");
        assertEquals(HttpStatus.CREATED, api.registro(solicitud).getStatusCode());
        verify(cuentas).registrarUsuario(solicitud.username(), solicitud.password(), "Ana", "123");
    }

    @Test
    void vinculaNuevaCuentaConElFuncionarioSolicitado() {
        var solicitud = new AuthController.CuentaFuncionario("pedro", "clave-de-prueba", 7L);
        assertEquals(HttpStatus.CREATED, api.funcionario(solicitud).getStatusCode());
        verify(cuentas).registrarFuncionario(solicitud.username(), solicitud.password(), 7L);
    }
}

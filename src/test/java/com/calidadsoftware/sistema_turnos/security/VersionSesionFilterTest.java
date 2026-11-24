package com.calidadsoftware.sistema_turnos.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionSesionFilterTest {
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cuentaRetiradaInvalidaSesionYLimpiaAutenticacion() throws Exception {
        var repositorio = mock(CuentaRepository.class);
        var principal = new CuentaPrincipal(1L, "ana", "hash", Rol.USUARIO, 2L, null);
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
        var peticion = new MockHttpServletRequest();
        var sesion = (MockHttpSession) peticion.getSession();
        var respuesta = new MockHttpServletResponse();
        var cadena = mock(FilterChain.class);
        new VersionSesionFilter(repositorio).doFilterInternal(peticion, respuesta, cadena);
        assertEquals(401, respuesta.getStatus());
        assertEquals("application/json;charset=UTF-8", respuesta.getContentType());
        assertTrue(respuesta.getContentAsString().contains("Inicia sesión nuevamente"));
        assertTrue(sesion.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(cadena);
    }
}

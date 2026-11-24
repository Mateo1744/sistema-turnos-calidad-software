package com.calidadsoftware.sistema_turnos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.calidadsoftware.sistema_turnos.repository.UsuarioRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.calidadsoftware.sistema_turnos.entity.Usuario;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;

class UsuarioServiceTest {

    @Test
    void creaUsuario() {
        var repository = mock(UsuarioRepository.class);
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocacion -> {
                    Usuario guardado = invocacion.getArgument(0);
                    assertNull(guardado.getId());
                    guardado.setId(1L);
                    return guardado;
                });
        UsuarioService usuarioService = new UsuarioService(repository);
        Usuario usuario = new Usuario(99L, " Ana ", " 123456 ", TipoUsuario.NORMAL);

        Usuario usuarioCreado = usuarioService.registrar(usuario);

        assertNotNull(usuarioCreado.getId());
        assertEquals("Ana", usuarioCreado.getNombre());
        assertEquals("123456", usuarioCreado.getDocumento());
        assertEquals(TipoUsuario.NORMAL, usuarioCreado.getTipoUsuario());
    }

    @Test
    void rechazaDocumentoExistenteAntesDeGuardar() {
        var repository = mock(UsuarioRepository.class);
        when(repository.existsByDocumentoIgnoreCase("123")).thenReturn(true);
        var servicio = new UsuarioService(repository);
        var usuario = new Usuario(null, "Ana", " 123 ", TipoUsuario.NORMAL);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(usuario));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).saveAndFlush(any());
    }
}

package com.calidadsoftware.sistema_turnos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.calidadsoftware.sistema_turnos.entity.Usuario;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;

class UsuarioServiceTest {

    @Test
    void creaUsuario() {
        UsuarioService usuarioService = new UsuarioService();
        Usuario usuario = new Usuario(null, "Ana", "123456", TipoUsuario.NORMAL);

        Usuario usuarioCreado = usuarioService.registrar(usuario);

        assertNotNull(usuarioCreado.getId());
        assertEquals("Ana", usuarioCreado.getNombre());
        assertEquals("123456", usuarioCreado.getDocumento());
        assertEquals(TipoUsuario.NORMAL, usuarioCreado.getTipoUsuario());
    }
}

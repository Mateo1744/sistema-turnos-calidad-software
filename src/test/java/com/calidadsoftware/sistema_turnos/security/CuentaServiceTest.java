package com.calidadsoftware.sistema_turnos.security;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;
import com.calidadsoftware.sistema_turnos.service.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CuentaServiceTest {
    private final CuentaRepository repositorio = mock(CuentaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final FuncionarioService funcionarios = mock(FuncionarioService.class);
    private final CuentaService cuentas = new CuentaService(repositorio, encoder, usuarios, funcionarios);
    private static final String CLAVE = "cuenta-de-prueba";

    @ParameterizedTest
    @ValueSource(ints = {12, 64})
    void aceptaLongitudesLimite(int longitud) {
        String clave = "a".repeat(longitud);
        cuentas.crearAdministradorInicial("admin-demo", clave);
        verify(encoder).encode(clave);
    }

    @Test
    void admiteHasta72BytesUtf8() {
        String limite = "ñ".repeat(36);
        cuentas.crearAdministradorInicial("admin-demo", limite);
        verify(encoder).encode(limite);
        String demasiadoLarga = limite + "a";
        var error = assertThrows(ResponseStatusException.class,
                () -> cuentas.crearAdministradorInicial("admin-demo", demasiadoLarga));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(ints = {11, 65})
    void rechazaClaveFueraDelRangoAntesDeCrearUsuario(int longitud) {
        String clave = "a".repeat(longitud);
        var error = assertThrows(ResponseStatusException.class,
                () -> cuentas.registrarUsuario("persona", clave, "Ana", "123"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(usuarios, encoder);
    }

    @Test
    void rechazaNombreOcupadoAntesDeCrearFuncionario() {
        when(repositorio.existsByUsername("ana")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
                () -> cuentas.registrarFuncionario(" ANA ", CLAVE, 5L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(funcionarios, encoder);
    }

    @Test
    void rechazaSegundaCuentaDelMismoFuncionario() {
        when(repositorio.existsByFuncionarioId(5L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
                () -> cuentas.registrarFuncionario("otra", CLAVE, 5L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void elRestablecimientoConservaLasCuentasDeUsuario() {
        var usuario = new Cuenta("ana", "hash-anterior", Rol.USUARIO, null, null);
        when(repositorio.findByUsername("ana")).thenReturn(Optional.of(usuario));
        var error = assertThrows(ResponseStatusException.class,
                () -> cuentas.restablecerAdministrador("ana", CLAVE));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("hash-anterior", usuario.getPasswordHash());
        verifyNoInteractions(encoder);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void rechazaClaveInvalidaAntesDeRestablecer() {
        var error = assertThrows(ResponseStatusException.class,
                () -> cuentas.restablecerAdministrador("admin", "corta"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repositorio, encoder);
    }

    @Test
    void identificaCuentaPorNombreNormalizadoYRechazaDesconocida() {
        var cuenta = new Cuenta("ana", "hash-prueba", Rol.ADMIN, null, null);
        when(repositorio.findByUsername("ana")).thenReturn(Optional.of(cuenta));
        assertEquals("ana", cuentas.loadUserByUsername(" ANA ").getUsername());
        assertThrows(UsernameNotFoundException.class, () -> cuentas.loadUserByUsername("otro"));
    }

    @Test
    void listadoPublicaRolEIdentificadorDeCadaPerfil() {
        var usuario = new Usuario(8L, "Ana", "123", TipoUsuario.NORMAL);
        var funcionario = new Funcionario(7L, "Pedro", "ACTIVO", null);
        when(repositorio.findAll()).thenReturn(List.of(
                new Cuenta("ana", "hash1", Rol.USUARIO, usuario, null),
                new Cuenta("pedro", "hash2", Rol.FUNCIONARIO, null, funcionario)));
        assertEquals(List.of(new AuthController.Perfil("ana", Rol.USUARIO, 8L, null),
                new AuthController.Perfil("pedro", Rol.FUNCIONARIO, null, 7L)), new AuthController(cuentas).listar());
    }
}

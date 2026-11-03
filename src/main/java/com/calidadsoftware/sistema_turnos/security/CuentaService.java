package com.calidadsoftware.sistema_turnos.security;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.Usuario;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;
import com.calidadsoftware.sistema_turnos.service.*;

@Service
@Transactional
public class CuentaService implements UserDetailsService {
    private final CuentaRepository cuentas;
    private final PasswordEncoder encoder;
    private final UsuarioService usuarios;
    private final FuncionarioService funcionarios;

    public CuentaService(CuentaRepository cuentas, PasswordEncoder encoder,
            UsuarioService usuarios, FuncionarioService funcionarios) {
        this.cuentas = cuentas; this.encoder = encoder; this.usuarios = usuarios; this.funcionarios = funcionarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var cuenta = cuentas.findByUsername(normalizar(username))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return new CuentaPrincipal(cuenta.getId(), cuenta.getUsername(), cuenta.getPasswordHash(), cuenta.getRol(),
                cuenta.getUsuario() == null ? null : cuenta.getUsuario().getId(),
                cuenta.getFuncionario() == null ? null : cuenta.getFuncionario().getId());
    }

    public void registrarUsuario(String username, String password, String nombre, String documento) {
        verificar(username, password);
        var usuario = usuarios.registrar(new Usuario(null, nombre, documento, TipoUsuario.NORMAL));
        cuentas.saveAndFlush(new Cuenta(normalizar(username), encoder.encode(password), Rol.USUARIO, usuario, null));
    }

    public void registrarFuncionario(String username, String password, Long funcionarioId) {
        verificar(username, password);
        var funcionario = funcionarios.buscarPorId(funcionarioId);
        if (cuentas.existsByFuncionarioId(funcionarioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El funcionario ya tiene cuenta");
        }
        cuentas.saveAndFlush(new Cuenta(normalizar(username), encoder.encode(password),
                Rol.FUNCIONARIO, null, funcionario));
    }

    public void crearAdministradorInicial(String username, String password) {
        if (cuentas.existsByRol(Rol.ADMIN)) return;
        verificar(username, password);
        cuentas.saveAndFlush(new Cuenta(normalizar(username), encoder.encode(password), Rol.ADMIN, null, null));
    }

    private void verificar(String username, String password) {
        validarFormato(username, password);
        if (cuentas.existsByUsername(normalizar(username))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede crear la cuenta con esos datos");
        }
    }

    private void validarFormato(String username, String password) {
        if (username == null || !normalizar(username).matches("[a-z0-9._-]{3,50}")
                || password == null || password.length() < 12 || password.length() > 64
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuario: 3 a 50 letras, números, punto, guion o guion bajo. Contraseña: 12 a 64 caracteres.");
        }
    }

    public void restablecerAdministrador(String username, String password) {
        validarFormato(username, password);
        var cuenta = cuentas.findByUsername(normalizar(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        if (cuenta.getRol() != Rol.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta no es administradora");
        }
        cuenta.cambiarPasswordHash(encoder.encode(password));
        cuentas.saveAndFlush(cuenta);
    }

    private String normalizar(String value) { return value.trim().toLowerCase(Locale.ROOT); }

    public java.util.List<AuthController.Perfil> listar() {
        return cuentas.findAll().stream().map(c -> new AuthController.Perfil(c.getUsername(), c.getRol(),
                c.getUsuario() == null ? null : c.getUsuario().getId(),
                c.getFuncionario() == null ? null : c.getFuncionario().getId())).toList();
    }
}

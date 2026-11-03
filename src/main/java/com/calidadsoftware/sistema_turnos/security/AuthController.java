package com.calidadsoftware.sistema_turnos.security;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
public class AuthController {
    private final CuentaService cuentas;
    public AuthController(CuentaService cuentas) { this.cuentas = cuentas; }

    public record Registro(@NotBlank @Size(min=3,max=50) String username,
            @NotBlank @Size(min=12,max=64) String password,
            @NotBlank @Size(max=160) String nombre, @NotBlank @Size(max=160) String documento) {}
    public record CuentaFuncionario(@NotBlank String username, @NotBlank @Size(min=12,max=64) String password,
            @NotNull @Positive Long funcionarioId) {}
    public record Perfil(String username, Rol rol, Long usuarioId, Long funcionarioId) {}

    @GetMapping("/api/auth/csrf")
    public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @GetMapping("/api/auth/me")
    public Perfil perfil(@AuthenticationPrincipal CuentaPrincipal principal) {
        return new Perfil(principal.username(), principal.rol(), principal.usuarioId(), principal.funcionarioId());
    }
    @PostMapping("/api/auth/registro")
    public ResponseEntity<Void> registro(@Valid @RequestBody Registro solicitud) {
        cuentas.registrarUsuario(solicitud.username(), solicitud.password(), solicitud.nombre(), solicitud.documento());
        return ResponseEntity.status(201).build();
    }
    @PostMapping("/api/admin/cuentas-funcionarios")
    public ResponseEntity<Void> funcionario(@Valid @RequestBody CuentaFuncionario solicitud) {
        cuentas.registrarFuncionario(solicitud.username(), solicitud.password(), solicitud.funcionarioId());
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/api/admin/cuentas")
    public java.util.List<Perfil> listar() { return cuentas.listar(); }
}

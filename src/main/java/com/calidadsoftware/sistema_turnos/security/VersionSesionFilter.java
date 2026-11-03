package com.calidadsoftware.sistema_turnos.security;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;

final class VersionSesionFilter extends OncePerRequestFilter {
    private final CuentaRepository cuentas;
    VersionSesionFilter(CuentaRepository cuentas) { this.cuentas = cuentas; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CuentaPrincipal principal) {
            var cuenta = cuentas.findById(principal.id());
            if (cuenta.isEmpty() || !cuenta.get().getPasswordHash().equals(principal.passwordHash())) {
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"detail\":\"Tu contraseña cambió. Inicia sesión nuevamente\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

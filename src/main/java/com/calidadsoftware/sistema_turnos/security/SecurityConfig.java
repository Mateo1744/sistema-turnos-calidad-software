package com.calidadsoftware.sistema_turnos.security;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, CuentaRepository cuentas) throws Exception {
        http.addFilterAfter(new VersionSesionFilter(cuentas),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/login.html", "/styles.css", "/app.js",
                        "/auth.js", "/auth.css", "/login.js", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/registro").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/mi/**").hasRole("USUARIO")
                .requestMatchers(HttpMethod.GET, "/servicios", "/servicios/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/turnos").hasRole("ADMIN")
                .requestMatchers("/turnos", "/turnos/**").hasAnyRole("ADMIN", "FUNCIONARIO")
                .requestMatchers(HttpMethod.GET, "/funcionarios", "/funcionarios/*").hasAnyRole("ADMIN", "FUNCIONARIO")
                .requestMatchers("/usuarios", "/usuarios/**", "/servicios", "/servicios/**",
                        "/funcionarios", "/funcionarios/**").hasRole("ADMIN")
                .anyRequest().denyAll());
        http.formLogin(form -> form.loginPage("/login.html").loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"ok\":true}");
                })
                .failureHandler((req, res, error) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"detail\":\"Usuario o contraseña incorrectos\"}");
                }));
        http.logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, error) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"detail\":\"Inicia sesión para continuar\"}");
                })
                .accessDeniedHandler((req, res, error) -> {
                    res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"detail\":\"No tienes permiso o falta un token CSRF válido\"}");
                }));
        // Se conserva la protección CSRF predeterminada de Spring Security.
        http.headers(headers -> headers.contentSecurityPolicy(policy -> policy.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'")));
        return http.build();
    }
}

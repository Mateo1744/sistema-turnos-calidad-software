package com.calidadsoftware.sistema_turnos.security;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AdminInicial {
    @Bean
    ApplicationRunner prepararAdmin(CuentaService cuentas,
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password) {
        return args -> {
            if (!username.isBlank() && !password.isBlank()) cuentas.crearAdministradorInicial(username, password);
        };
    }
}

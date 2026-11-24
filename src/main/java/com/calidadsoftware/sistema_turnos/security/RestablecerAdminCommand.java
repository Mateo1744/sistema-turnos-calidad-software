package com.calidadsoftware.sistema_turnos.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.calidadsoftware.sistema_turnos.SistemaTurnosApplication;

/** Herramienta local de mantenimiento; no expone un endpoint público. */
public final class RestablecerAdminCommand {
    private static final Logger LOG = LoggerFactory.getLogger(RestablecerAdminCommand.class);

    public static void main(String[] args) {
        new RestablecerAdminCommand().ejecutar(System.getenv("RESET_ADMIN_USERNAME"), System.getenv("RESET_ADMIN_PASSWORD"));
    }

    RestablecerAdminCommand() {}

    void ejecutar(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Ejecuta scripts/restablecer-admin.ps1 para introducir los datos localmente");
        }
        try (var context = new SpringApplicationBuilder(SistemaTurnosApplication.class)
                .web(WebApplicationType.NONE).run("--app.admin.username=", "--app.admin.password=")) {
            context.getBean(CuentaService.class).restablecerAdministrador(username, password);
            LOG.info("Contraseña del administrador actualizada. Ya puedes iniciar la aplicación normalmente.");
        }
    }
}

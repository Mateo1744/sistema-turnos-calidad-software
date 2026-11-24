package com.calidadsoftware.sistema_turnos.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestablecerAdminCommandTest {
    @Test
    void mainEntregaLasCredencialesDelEntornoAlComando() {
        // Interceptar el comando evita abrir una conexión con la base instalada.
        try (var comandos = mockConstruction(RestablecerAdminCommand.class)) {
            RestablecerAdminCommand.main(new String[0]);
            verify(comandos.constructed().get(0)).ejecutar(
                    System.getenv("RESET_ADMIN_USERNAME"), System.getenv("RESET_ADMIN_PASSWORD"));
        }
    }

    @Test
    void exigeAmbosDatosAntesDeAbrirElContexto() {
        var comando = new RestablecerAdminCommand();
        try (var builders = mockConstruction(SpringApplicationBuilder.class)) {
            assertThrows(IllegalArgumentException.class, () -> comando.ejecutar(null, "clave-de-prueba"));
            assertThrows(IllegalArgumentException.class, () -> comando.ejecutar("admin-demo", null));
            assertTrue(builders.constructed().isEmpty());
        }
    }

    @Test
    void restableceEnModoLocalYCierraElContexto() {
        var contexto = mock(ConfigurableApplicationContext.class);
        var cuentas = mock(CuentaService.class);
        when(contexto.getBean(CuentaService.class)).thenReturn(cuentas);
        try (var builders = mockConstruction(SpringApplicationBuilder.class, (builder, construction) -> {
            when(builder.web(WebApplicationType.NONE)).thenReturn(builder);
            when(builder.run("--app.admin.username=", "--app.admin.password=")).thenReturn(contexto);
        })) {
            new RestablecerAdminCommand().ejecutar("admin-demo", "clave-de-prueba");
            verify(cuentas).restablecerAdministrador("admin-demo", "clave-de-prueba");
            verify(contexto).close();
        }
    }
}

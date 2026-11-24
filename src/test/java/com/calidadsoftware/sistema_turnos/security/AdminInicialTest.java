package com.calidadsoftware.sistema_turnos.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.Mockito.*;

class AdminInicialTest {
    private final CuentaService cuentas = mock(CuentaService.class);
    private final AdminInicial inicial = new AdminInicial();

    @Test
    void creaAdminCuandoSeConfiguranAmbosDatos() throws Exception {
        inicial.prepararAdmin(cuentas, "demo", "clave-de-prueba").run(null);
        verify(cuentas).crearAdministradorInicial("demo", "clave-de-prueba");
    }

    @ParameterizedTest
    @CsvSource({"'',clave-de-prueba", "demo,''"})
    void omiteElAltaConConfiguracionIncompleta(String usuario, String clave) throws Exception {
        inicial.prepararAdmin(cuentas, usuario, clave).run(null);
        verifyNoInteractions(cuentas);
    }
}

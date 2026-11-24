package com.calidadsoftware.sistema_turnos.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import com.calidadsoftware.sistema_turnos.service.FuncionarioService;
import com.calidadsoftware.sistema_turnos.service.TurnoService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AccesoTurnosTest {
    @Test
    void deniegaAccesoSinAutenticacion() {
        var funcionarios = mock(FuncionarioService.class);
        var acceso = new AccesoTurnos(funcionarios, mock(TurnoService.class));
        var anterior = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.clearContext();
            assertFalse(acceso.admin());
            assertThrows(AccessDeniedException.class, acceso::funcionario);
            verifyNoInteractions(funcionarios);
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }
}

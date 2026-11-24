package com.calidadsoftware.sistema_turnos.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.calidadsoftware.sistema_turnos.entity.Servicio;
import com.calidadsoftware.sistema_turnos.repository.FuncionarioRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FuncionarioServiceTest {
    @Test
    void rechazaElAltaEnUnServicioInactivo() {
        var repositorio = mock(FuncionarioRepository.class);
        var servicios = mock(ServicioService.class);
        when(servicios.buscarPorId(3L)).thenReturn(new Servicio(3L, "Caja", "Pagos", "INACTIVO"));
        var funcionarios = new FuncionarioService(servicios, repositorio);
        var error = assertThrows(ResponseStatusException.class, () -> funcionarios.registrar("Ana", 3L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(repositorio);
    }
}

package com.calidadsoftware.sistema_turnos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.calidadsoftware.sistema_turnos.repository.ServicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;

import com.calidadsoftware.sistema_turnos.entity.Servicio;

class ServicioServiceTest {

    @Test
    void creaServicio() {
        var repository = mock(ServicioRepository.class);
        when(repository.saveAndFlush(any(Servicio.class)))
                .thenAnswer(invocacion -> {
                    Servicio guardado = invocacion.getArgument(0);
                    assertNull(guardado.getId());
                    guardado.setId(1L);
                    return guardado;
                });
        ServicioService servicioService = new ServicioService(repository);
        Servicio servicio = new Servicio(99L, " Pagos ", " Pago de facturas ", "activo");

        Servicio servicioCreado = servicioService.registrar(servicio);

        assertNotNull(servicioCreado.getId());
        assertEquals("Pagos", servicioCreado.getNombre());
        assertEquals("Pago de facturas", servicioCreado.getDescripcion());
        assertEquals("ACTIVO", servicioCreado.getEstado());
    }

    @Test
    void rechazaServicioDuplicado() {
        var repositorio = mock(ServicioRepository.class);
        when(repositorio.existsByNombreIgnoreCase("Pagos")).thenReturn(true);
        var servicio = new Servicio(null, " Pagos ", "Caja", "activo");
        var servicioService = new ServicioService(repositorio);
        var error = assertThrows(
                ResponseStatusException.class,
                () -> servicioService.registrar(servicio));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).saveAndFlush(any());
    }
}

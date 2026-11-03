package com.calidadsoftware.sistema_turnos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.calidadsoftware.sistema_turnos.entity.Servicio;

class ServicioServiceTest {

    @Test
    void creaServicio() {
        var repository = org.mockito.Mockito.mock(
                com.calidadsoftware.sistema_turnos.repository.ServicioRepository.class);
        org.mockito.Mockito.when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(Servicio.class)))
                .thenAnswer(invocacion -> {
                    Servicio guardado = invocacion.getArgument(0);
                    guardado.setId(1L);
                    return guardado;
                });
        ServicioService servicioService = new ServicioService(repository);
        Servicio servicio = new Servicio(null, "Pagos", "Pago de facturas", "activo");

        Servicio servicioCreado = servicioService.registrar(servicio);

        assertNotNull(servicioCreado.getId());
        assertEquals("Pagos", servicioCreado.getNombre());
        assertEquals("Pago de facturas", servicioCreado.getDescripcion());
        assertEquals("ACTIVO", servicioCreado.getEstado());
    }
}

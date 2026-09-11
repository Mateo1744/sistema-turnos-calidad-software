package com.calidadsoftware.sistema_turnos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.calidadsoftware.sistema_turnos.entity.Servicio;

class ServicioServiceTest {

    @Test
    void creaServicio() {
        ServicioService servicioService = new ServicioService();
        Servicio servicio = new Servicio(null, "Pagos", "Pago de facturas", "activo");

        Servicio servicioCreado = servicioService.registrar(servicio);

        assertNotNull(servicioCreado.getId());
        assertEquals("Pagos", servicioCreado.getNombre());
        assertEquals("Pago de facturas", servicioCreado.getDescripcion());
        assertEquals("ACTIVO", servicioCreado.getEstado());
    }
}

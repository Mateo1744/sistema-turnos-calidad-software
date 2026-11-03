package com.calidadsoftware.sistema_turnos.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.calidadsoftware.sistema_turnos.entity.Servicio;

@Service
public class ServicioService {

    private final com.calidadsoftware.sistema_turnos.repository.ServicioRepository servicios;

    public ServicioService(com.calidadsoftware.sistema_turnos.repository.ServicioRepository servicios) {
        this.servicios = servicios;
    }

    public Servicio buscarPorId(Long id) {
        return servicios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
    }

    public Servicio registrar(Servicio servicio) {
        String nombre = servicio.getNombre().trim();
        boolean nombreRegistrado = servicios.existsByNombreIgnoreCase(nombre);

        if (nombreRegistrado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio ya está registrado");
        }

        servicio.setId(null);
        servicio.setNombre(nombre);
        servicio.setDescripcion(servicio.getDescripcion().trim());
        servicio.setEstado(servicio.getEstado().trim().toUpperCase());
        return servicios.saveAndFlush(servicio);
    }

    public List<Servicio> listar() {
        return servicios.findAll(org.springframework.data.domain.Sort.by("id"));
    }
}

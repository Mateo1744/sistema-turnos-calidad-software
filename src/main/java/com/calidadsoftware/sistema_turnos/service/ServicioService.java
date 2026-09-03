package com.calidadsoftware.sistema_turnos.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.calidadsoftware.sistema_turnos.entity.Servicio;

@Service
public class ServicioService {

    private final List<Servicio> servicios = new ArrayList<>();
    private long siguienteId = 1;

    public Servicio registrar(Servicio servicio) {
        String nombre = servicio.getNombre().trim();
        boolean nombreRegistrado = servicios.stream()
                .anyMatch(existente -> existente.getNombre().equalsIgnoreCase(nombre));

        if (nombreRegistrado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio ya está registrado");
        }

        servicio.setId(siguienteId++);
        servicio.setNombre(nombre);
        servicio.setDescripcion(servicio.getDescripcion().trim());
        servicio.setEstado(servicio.getEstado().trim().toUpperCase());
        servicios.add(servicio);
        return servicio;
    }

    public List<Servicio> listar() {
        return List.copyOf(servicios);
    }
}

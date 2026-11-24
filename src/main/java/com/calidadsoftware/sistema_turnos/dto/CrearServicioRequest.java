package com.calidadsoftware.sistema_turnos.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearServicioRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La descripción es obligatoria") String descripcion,
        @NotBlank(message = "El estado es obligatorio") String estado) {
}

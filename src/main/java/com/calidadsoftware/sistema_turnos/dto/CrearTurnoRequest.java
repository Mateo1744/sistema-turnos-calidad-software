package com.calidadsoftware.sistema_turnos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearTurnoRequest(
        @NotNull @Positive Long usuarioId,
        @NotNull @Positive Long servicioId) {
}

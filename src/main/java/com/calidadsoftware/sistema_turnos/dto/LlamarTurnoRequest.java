package com.calidadsoftware.sistema_turnos.dto;

import jakarta.validation.constraints.*;

public record LlamarTurnoRequest(@NotNull @Positive Long funcionarioId) {}

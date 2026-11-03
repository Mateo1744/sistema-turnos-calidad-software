package com.calidadsoftware.sistema_turnos.dto;

import jakarta.validation.constraints.*;

public record CrearFuncionarioRequest(@NotBlank String nombre, @NotNull @Positive Long servicioId) {}

package com.calidadsoftware.sistema_turnos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;

public record CrearUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El documento es obligatorio") String documento,
        @NotNull(message = "El tipo de usuario es obligatorio") TipoUsuario tipoUsuario) {
}

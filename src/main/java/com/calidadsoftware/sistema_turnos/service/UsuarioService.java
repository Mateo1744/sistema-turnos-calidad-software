package com.calidadsoftware.sistema_turnos.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.calidadsoftware.sistema_turnos.entity.Usuario;

@Service
public class UsuarioService {

    private final List<Usuario> usuarios = new ArrayList<>();
    private long siguienteId = 1;

    public Usuario registrar(Usuario usuario) {
        String documento = usuario.getDocumento().trim();
        boolean documentoRegistrado = usuarios.stream()
                .anyMatch(existente -> existente.getDocumento().equalsIgnoreCase(documento));

        if (documentoRegistrado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya está registrado");
        }

        usuario.setId(siguienteId++);
        usuario.setNombre(usuario.getNombre().trim());
        usuario.setDocumento(documento);
        usuarios.add(usuario);
        return usuario;
    }
}

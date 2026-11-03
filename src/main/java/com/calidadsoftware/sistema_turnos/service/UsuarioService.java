package com.calidadsoftware.sistema_turnos.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.calidadsoftware.sistema_turnos.entity.Usuario;

@Service
public class UsuarioService {

    private final com.calidadsoftware.sistema_turnos.repository.UsuarioRepository usuarios;

    public UsuarioService(com.calidadsoftware.sistema_turnos.repository.UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public List<Usuario> listar() { return usuarios.findAll(org.springframework.data.domain.Sort.by("id")); }

    public Usuario buscarPorId(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario registrar(Usuario usuario) {
        String documento = usuario.getDocumento().trim();
        boolean documentoRegistrado = usuarios.existsByDocumentoIgnoreCase(documento);

        if (documentoRegistrado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya está registrado");
        }

        usuario.setId(null);
        usuario.setNombre(usuario.getNombre().trim());
        usuario.setDocumento(documento);
        return usuarios.saveAndFlush(usuario);
    }
}

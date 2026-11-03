package com.calidadsoftware.sistema_turnos.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.calidadsoftware.sistema_turnos.entity.Usuario;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByDocumentoIgnoreCase(String documento);
}

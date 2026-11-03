package com.calidadsoftware.sistema_turnos.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByRol(Rol rol);
    boolean existsByFuncionarioId(Long id);
}

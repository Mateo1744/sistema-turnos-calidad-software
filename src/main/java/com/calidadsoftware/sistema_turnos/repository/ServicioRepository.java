package com.calidadsoftware.sistema_turnos.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.calidadsoftware.sistema_turnos.entity.Servicio;
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}

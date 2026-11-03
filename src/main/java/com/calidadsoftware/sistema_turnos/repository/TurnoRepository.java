package com.calidadsoftware.sistema_turnos.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.calidadsoftware.sistema_turnos.entity.Turno;
public interface TurnoRepository extends JpaRepository<Turno, Long> {}

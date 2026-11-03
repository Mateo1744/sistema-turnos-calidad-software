package com.calidadsoftware.sistema_turnos.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.calidadsoftware.sistema_turnos.entity.Funcionario;
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {}

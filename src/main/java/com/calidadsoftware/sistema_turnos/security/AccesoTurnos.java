package com.calidadsoftware.sistema_turnos.security;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.calidadsoftware.sistema_turnos.entity.*;
import com.calidadsoftware.sistema_turnos.service.*;

@Component
public class AccesoTurnos {
    private final FuncionarioService funcionarios;
    private final TurnoService turnos;
    public AccesoTurnos(FuncionarioService funcionarios, TurnoService turnos) {
        this.funcionarios = funcionarios; this.turnos = turnos;
    }
    public boolean admin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    public Funcionario funcionario() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CuentaPrincipal cuenta) || cuenta.funcionarioId() == null) {
            throw new AccessDeniedException("No tienes un funcionario asociado");
        }
        var funcionario = funcionarios.buscarPorId(cuenta.funcionarioId());
        if (!"ACTIVO".equals(funcionario.getEstado())) throw new AccessDeniedException("Funcionario inactivo");
        return funcionario;
    }
    public boolean visible(Turno turno) {
        return admin() || turno.getServicio().getId().equals(funcionario().getServicio().getId());
    }
    public void verificar(Long id, boolean requiereAsignacion) {
        if (admin()) return;
        var turno = turnos.buscarPorId(id);
        var funcionario = funcionario();
        if (!visible(turno) || (requiereAsignacion
                && (turno.getFuncionario() == null || !turno.getFuncionario().getId().equals(funcionario.getId())))) {
            throw new AccessDeniedException("El turno no te corresponde");
        }
    }
    public void verificarFuncionario(Long id) {
        if (!admin() && !funcionario().getId().equals(id)) throw new AccessDeniedException("Funcionario no autorizado");
    }
}

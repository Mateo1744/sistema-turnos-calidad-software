package com.calidadsoftware.sistema_turnos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.calidadsoftware.sistema_turnos.entity.Turno;
import com.calidadsoftware.sistema_turnos.enums.EstadoTurno;

@Service
@org.springframework.transaction.annotation.Transactional
public class TurnoService {
    private final UsuarioService usuarios;
    private final ServicioService servicios;
    private final FuncionarioService funcionarios;
    private final com.calidadsoftware.sistema_turnos.repository.TurnoRepository turnos;

    public TurnoService(UsuarioService usuarios, ServicioService servicios, FuncionarioService funcionarios,
            com.calidadsoftware.sistema_turnos.repository.TurnoRepository turnos) {
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.funcionarios = funcionarios;
        this.turnos = turnos;
    }

    public synchronized Turno crear(Long usuarioId, Long servicioId) {
        var usuario = usuarios.buscarPorId(usuarioId);
        var servicio = servicios.buscarPorId(servicioId);
        if (!"ACTIVO".equalsIgnoreCase(servicio.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio no está activo");
        }
        boolean duplicado = turnos.findAll().stream().anyMatch(turno ->
                turno.getUsuario().getId().equals(usuarioId)
                && turno.getServicio().getId().equals(servicioId) && estaActivo(turno));
        if (duplicado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario ya tiene un turno activo para este servicio");
        }
        Turno turno = new Turno(null, null,
                usuario, servicio, null, LocalDateTime.now(), null, null, EstadoTurno.GENERADO);
        turnos.saveAndFlush(turno);
        turno.setNumero(String.format(Locale.ROOT, "T%03d", turno.getId()));
        turnos.saveAndFlush(turno);
        return turno;
    }

    public synchronized List<Turno> listar(Long servicioId) {
        if (servicioId == null) {
            return turnos.findAll(org.springframework.data.domain.Sort.by("id"));
        }
        servicios.buscarPorId(servicioId);
        return turnos.findAll(org.springframework.data.domain.Sort.by("id")).stream()
                .filter(t -> t.getServicio().getId().equals(servicioId)).toList();
    }

    public synchronized List<Turno> pendientes() {
        return turnos.findAll(org.springframework.data.domain.Sort.by("id")).stream().filter(t -> t.getEstado() == EstadoTurno.GENERADO
                || t.getEstado() == EstadoTurno.EN_ESPERA || t.getEstado() == EstadoTurno.LLAMADO).toList();
    }

    public synchronized Turno buscarPorId(Long id) {
        return turnos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
    }

    public synchronized Turno buscarPorNumero(String numero) {
        return turnos.findAll().stream().filter(t -> t.getNumero().equalsIgnoreCase(numero)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
    }

    private boolean estaActivo(Turno turno) {
        return turno.getEstado() != EstadoTurno.FINALIZADO && turno.getEstado() != EstadoTurno.CANCELADO;
    }

    public synchronized Turno ponerEnEspera(Long id) {
        var turno = buscarPorId(id);
        exigirEstado(turno, EstadoTurno.GENERADO);
        turno.setEstado(EstadoTurno.EN_ESPERA);
        return turno;
    }

    public synchronized Turno llamar(Long id, Long funcionarioId) {
        var turno = buscarPorId(id);
        exigirEstado(turno, EstadoTurno.EN_ESPERA);
        var funcionario = funcionarios.buscarPorId(funcionarioId);
        if (!"ACTIVO".equals(funcionario.getEstado())
                || !funcionario.getServicio().getId().equals(turno.getServicio().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Funcionario inactivo o de otro servicio");
        }
        boolean ocupado = turnos.findAll().stream().anyMatch(t -> t.getFuncionario() != null
                && t.getFuncionario().getId().equals(funcionarioId)
                && (t.getEstado() == EstadoTurno.LLAMADO || t.getEstado() == EstadoTurno.EN_ATENCION));
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El funcionario ya tiene un turno asignado");
        }
        var siguiente = turnos.findAll().stream()
                .filter(t -> t.getEstado() == EstadoTurno.EN_ESPERA
                        && t.getServicio().getId().equals(turno.getServicio().getId()))
                .min(java.util.Comparator
                        .comparingInt((Turno t) -> t.getUsuario().getTipoUsuario()
                                == com.calidadsoftware.sistema_turnos.enums.TipoUsuario.PRIORITARIO ? 0 : 1)
                        .thenComparing(Turno::getId)).orElseThrow();
        if (!siguiente.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Debe llamar primero al turno " + siguiente.getNumero());
        }
        turno.setFuncionario(funcionario);
        turno.setEstado(EstadoTurno.LLAMADO);
        return turno;
    }

    public synchronized Turno iniciar(Long id) {
        var turno = buscarPorId(id);
        exigirEstado(turno, EstadoTurno.LLAMADO);
        turno.setFechaInicio(LocalDateTime.now());
        turno.setEstado(EstadoTurno.EN_ATENCION);
        return turno;
    }

    public synchronized Turno finalizar(Long id) {
        var turno = buscarPorId(id);
        exigirEstado(turno, EstadoTurno.EN_ATENCION);
        var ahora = LocalDateTime.now();
        if (ahora.isBefore(turno.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fecha de finalización anterior al inicio");
        }
        turno.setFechaFinalizacion(ahora);
        turno.setEstado(EstadoTurno.FINALIZADO);
        return turno;
    }

    public synchronized Turno cancelar(Long id) {
        var turno = buscarPorId(id);
        if (!estaActivo(turno)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya está cerrado");
        }
        turno.setEstado(EstadoTurno.CANCELADO);
        return turno;
    }

    public synchronized List<Turno> porFuncionario(Long id) {
        funcionarios.buscarPorId(id);
        return turnos.findAll(org.springframework.data.domain.Sort.by("id")).stream().filter(t -> t.getFuncionario() != null
                && t.getFuncionario().getId().equals(id)).toList();
    }

    private void exigirEstado(Turno turno, EstadoTurno esperado) {
        if (turno.getEstado() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Se requiere estado " + esperado + "; estado actual: " + turno.getEstado());
        }
    }
}

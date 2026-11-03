-- Protecciones adicionales específicas de PostgreSQL.
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_documento_ci ON usuarios (lower(documento));
CREATE UNIQUE INDEX IF NOT EXISTS ux_servicio_nombre_ci ON servicios (lower(nombre));
CREATE UNIQUE INDEX IF NOT EXISTS ux_turno_activo_usuario_servicio
    ON turnos (usuario_id, servicio_id)
    WHERE estado NOT IN ('FINALIZADO', 'CANCELADO');
CREATE UNIQUE INDEX IF NOT EXISTS ux_funcionario_ocupado
    ON turnos (funcionario_id)
    WHERE funcionario_id IS NOT NULL AND estado IN ('LLAMADO', 'EN_ATENCION');

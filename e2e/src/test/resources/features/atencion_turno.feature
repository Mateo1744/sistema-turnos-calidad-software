# language: es
@e2e @turnos
Característica: Atención completa de un turno
  Como administrador del centro de atención
  Quiero registrar y atender un turno
  Para completar la solicitud de un usuario

  Escenario: Crear un turno y completar todos los estados de atención
    Dado que abro el login del Sistema de Turnos
    Cuando inicio sesión como administrador de pruebas
    Entonces veo el centro de turnos y mi rol de administrador
    Cuando registro un servicio activo, un usuario y un funcionario desde la interfaz
    Y creo un turno para ese usuario y servicio
    Entonces el turno aparece en estado "generado"
    Cuando pongo el turno en espera
    Entonces el turno aparece en estado "en espera"
    Cuando llamo el turno con el funcionario registrado
    Entonces el turno aparece en estado "llamado"
    Cuando inicio la atención del turno
    Entonces el turno aparece en estado "en atencion"
    Cuando finalizo la atención del turno
    Entonces el turno aparece en estado "finalizado"
    Y después de recargar conserva el estado finalizado y el funcionario asignado

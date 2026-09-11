# language: es
Característica: Registro de servicios

  Escenario: Registrar un servicio activo
    Dado que deseo registrar el servicio "Pagos" con descripción "Recepción de pagos presenciales" y estado "activo"
    Cuando registro el servicio
    Entonces el servicio queda creado con estado "ACTIVO" y un identificador

# language: es
Característica: Registro de usuarios

  Escenario: Registrar un usuario normal
    Dado que deseo registrar un usuario llamado "Mateo" con documento "123456789" y tipo "NORMAL"
    Cuando registro el usuario
    Entonces el usuario queda creado con nombre "Mateo" y un identificador

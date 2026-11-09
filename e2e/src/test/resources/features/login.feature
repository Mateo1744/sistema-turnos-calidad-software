# language: es
@e2e @login
Característica: Acceso seguro al Sistema de Turnos
  Como administrador
  Quiero entrar y salir de mi cuenta
  Para gestionar la atención y cerrar mi sesión al terminar

  Escenario: Iniciar sesión y cerrar el acceso al panel
    Dado que abro el login del Sistema de Turnos
    Cuando inicio sesión como administrador de pruebas
    Entonces veo el centro de turnos y mi rol de administrador
    Cuando cierro sesión
    Entonces regreso al login con la confirmación de salida
    Y al intentar abrir el panel vuelvo al login

  Escenario: Rechazar una contraseña incorrecta
    Dado que abro el login del Sistema de Turnos
    Cuando ingreso una contraseña incorrecta
    Entonces veo un error de autenticación y permanezco en el login

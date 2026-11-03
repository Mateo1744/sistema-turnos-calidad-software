# API de atención

Ejecutar desde la raíz: `./gradlew bootRun` (Windows: `.\gradlew.bat bootRun`).
Dirección local: http://localhost:8080.

Los datos se guardan en PostgreSQL mediante JPA. Consultar BASE-DE-DATOS.md para
crear la base y configurar las credenciales antes de iniciar.
Esta versión usa login de sesión y roles (ver LOGIN.md). Las operaciones de
gestión requieren ADMIN o el permiso de funcionario correspondiente.
No se ha verificado todo el flujo entre varias instancias.

## Flujo para el frontend o Postman

Enviar JSON con Content-Type: application/json:

1. POST /usuarios: {"nombre":"Ana","documento":"100","tipoUsuario":"NORMAL"}
2. POST /servicios: {"nombre":"Pagos","descripcion":"Pago de facturas","estado":"ACTIVO"}
3. POST /funcionarios: {"nombre":"Laura","servicioId":1}
4. POST /turnos: {"usuarioId":1,"servicioId":1}
5. PATCH /turnos/1/esperar (sin cuerpo).
6. PATCH /turnos/1/llamar: {"funcionarioId":1}
7. PATCH /turnos/1/iniciar (sin cuerpo).
8. PATCH /turnos/1/finalizar (sin cuerpo).

Usar los identificadores recibidos en cada respuesta, no asumir siempre 1.
PATCH /turnos/{id}/cancelar cancela cualquier turno todavía activo, incluso
en atención. Un turno cerrado no puede reabrirse ni cancelarse nuevamente.

## Consultas

- GET /usuarios y GET /usuarios/{id}
- GET /servicios y GET /servicios/{id}
- GET /funcionarios y GET /funcionarios/{id}
- GET /turnos y GET /turnos/{id}
- GET /turnos?servicioId=1
- GET /turnos/numero/T001
- GET /turnos/pendientes (GENERADO, EN_ESPERA y LLAMADO)
- GET /turnos/funcionario/1 (incluye historial)

## Reglas y errores

Se exige GENERADO → EN_ESPERA → LLAMADO → EN_ATENCION → FINALIZADO.
Dentro de la cola EN_ESPERA de cada servicio se llama primero a PRIORITARIO;
para igual prioridad se utiliza el orden de creación.
El funcionario debe estar activo y pertenecer al servicio. Se reserva desde
LLAMADO hasta FINALIZADO o CANCELADO.

No se admiten dos turnos activos del mismo usuario para el mismo servicio.
Los números son únicos y dependen del identificador persistente de la base.
No se reinician al apagar la aplicación y pueden contener saltos.
Las fechas de inicio y finalización las determina el servidor.

Respuestas: 201 al crear, 200 al consultar o cambiar estado, 400 por datos
inválidos, 404 si el recurso no existe y 409 por conflicto de negocio.
Los errores de negocio contienen status y detail; los errores de validación
incluyen además errores con los campos afectados.

Verificar: `.\gradlew.bat test acceptanceTest`.

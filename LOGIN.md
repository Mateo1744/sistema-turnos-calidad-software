# Acceso al sistema

La cuenta postgres sirve para conectar con la base de datos. No es una cuenta
para entrar a la aplicación. Los accesos se guardan en la nueva tabla cuentas;
el hash BCrypt de la contraseña se guarda en password_hash.

## Primer administrador

Detener la aplicación anterior con Ctrl+C. Desde la raíz ejecutar:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-postgres.ps1 -CrearAdmin
~~~

El script solicita la contraseña de postgres, un nombre para el administrador
y una contraseña nueva de 12 a 64 caracteres. No guardar estos datos en Git.
El nombre permite letras, números, puntos, guiones y guion bajo (3 a 50).
BCrypt admite un máximo de 72 bytes UTF-8; evitar contraseñas más largas que eso.
Si ya existe un administrador, el proceso no crea otro ni cambia su contraseña.
No hay contraseña predeterminada.

Abrir http://localhost:8080 e ingresar con el administrador elegido.
El administrador queda guardado en la base. No repetir -CrearAdmin para entrar.
Para utilizar el comando normal, guardar una vez la conexión local:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\configurar-conexion.ps1
~~~

Este paso pide solo la contraseña de PostgreSQL y comprueba la conexión.
La guarda cifrada mediante DPAPI en .local/postgres.clixml, excluido de Git,
para el mismo usuario de Windows y el mismo equipo.
Después, en cualquier terminal de VS Code dentro del proyecto:

~~~powershell
.\gradlew.bat bootRun
~~~

Gradle recupera la conexión local si DB_PASSWORD no está definida.
Las variables de entorno tienen prioridad. En Azure o Linux configurar DB_URL,
DB_USERNAME y DB_PASSWORD como secretos del entorno; no copiar el archivo cifrado.
Si cambia la contraseña de PostgreSQL, ejecutar configurar-conexion.ps1 de nuevo.

El botón Cerrar sesión está arriba a la derecha, también en celular.
Invalida la sesión en el servidor y vuelve al login mostrando una confirmación.
Si se usa Atrás, el panel vuelve a comprobar el acceso.

## Restablecer un administrador

Si la consulta SELECT id, username, rol FROM cuentas WHERE rol = 'ADMIN'
confirma que existe la cuenta y no recuerdas su contraseña, detener la aplicación
y ejecutar desde la raíz:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\restablecer-admin.ps1
~~~

Introducir el username de pgAdmin y la nueva contraseña dos veces. Se usa la
conexión cifrada que ya guardaste para bootRun. La herramienta actualiza solo
una cuenta ADMIN existente, conserva su id y no crea otra cuenta.
No guarda contraseñas nuevas en archivos ni las pasa como argumentos de comandos.
Las sesiones que contenían el hash anterior se invalidan en su siguiente petición.

Después iniciar normalmente:

~~~powershell
.\gradlew.bat bootRun
~~~

Entrar con el mismo username y la nueva contraseña. La contraseña de postgres
no cambia. Si no existe configuración local, ejecutar configurar-conexion.ps1.

## Cuentas de funcionarios

Como administrador:

1. Registrar un servicio activo.
2. En Funcionarios, registrar a la persona y su servicio.
3. En Cuentas de acceso, pulsar Habilitar funcionario.
4. Elegir la ficha, asignarle un nombre de usuario y una contraseña.
5. Cerrar sesión e ingresar con esa cuenta para comprobar su vista.

La ficha de funcionario por sí sola no habilita un acceso.
Las fichas y usuarios que existían antes del login se conservan, pero no tienen
contraseña automáticamente. Para un documento ya registrado, todavía hace falta
un flujo administrativo de vinculación de cuenta; no se permite reclamar una
identidad existente solo por conocer su documento.

## Usuarios

En la pantalla de login, Regístrate crea una cuenta USUARIO con su propia ficha
NORMAL. No puede elegir un rol administrativo ni declararse PRIORITARIO.
Cada usuario solicita sus turnos por servicio y ve únicamente sus solicitudes.
Los turnos solicitados por usuarios pasan a EN_ESPERA automáticamente.
Puede cancelar mientras todavía no hayan sido llamados.

## Permisos

- ADMIN: catálogos, todos los turnos y habilitación de cuentas de funcionarios.
- FUNCIONARIO: ve los turnos de su servicio y atiende los asignados a sí mismo.
- USUARIO: servicios y solicitudes propias; sin acceso al panel de gestión.

Las comprobaciones se hacen en el servidor. Cambiar botones o enviar peticiones
manuales no cambia los permisos. NORMAL/PRIORITARIO son tipos de atención,
distintos de los roles ADMIN/FUNCIONARIO/USUARIO.

## API y pruebas

GET /api/auth/csrf devuelve token y headerName para POST/PATCH.
POST /api/auth/login recibe username y password como formulario URL-encoded.
GET /api/auth/me devuelve el rol y referencias de la sesión.
POST /api/auth/logout invalida la sesión.
POST /api/auth/registro crea exclusivamente USUARIO.
POST /api/admin/cuentas-funcionarios habilita una ficha existente (solo ADMIN).
GET /api/admin/cuentas lista únicamente perfiles, sin hashes.
GET/POST /api/mi/turnos y PATCH /api/mi/turnos/{id}/cancelar son para USUARIO.

Guardar la cookie de sesión y renovar el token CSRF después del login. La
interfaz lo hace automáticamente; Postman también debe enviar ambos.
Las rutas previas /usuarios, /funcionarios y /turnos ahora requieren permisos.
Las pruebas funcionales existentes usan un administrador simulado y token CSRF.
SeguridadIntegrationTests prueba el login real, hashing, logout, CSRF y permisos.

Sesión de 30 minutos de inactividad, cookie HttpOnly y SameSite=Lax, sin
contraseñas ni tokens en localStorage. Para publicar se necesita HTTPS y
SESSION_COOKIE_SECURE=true. Esta etapa no implementa recuperación de contraseña por correo para usuarios,
MFA, bloqueo por intentos ni auditoría de accesos.

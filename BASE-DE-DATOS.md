# PostgreSQL local

Requisito: PostgreSQL 18 encendido, usuario postgres y su contraseña local.
Desde la raíz del proyecto en PowerShell:

~~~powershell
.\scripts\iniciar-postgres.ps1
~~~

El script solicita la contraseña sin mostrarla, crea sistema_turnos únicamente si
no existe e inicia Spring Boot. No guarda credenciales en archivos. Si PowerShell
bloquea el archivo, puede ejecutarse solo para ese proceso:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-postgres.ps1
~~~

Abrir http://localhost:8080. Detener con Ctrl+C.
Para el uso diario, guardar la conexión una vez:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\configurar-conexion.ps1
~~~

La contraseña se guarda cifrada con Windows en .local/postgres.clixml,
excluido de Git. Después ejecutar normalmente .\gradlew.bat bootRun.
No se guarda la contraseña del administrador en esta configuración.
La aplicación también acepta DB_URL, DB_USERNAME y DB_PASSWORD como variables
de entorno si se inicia de otra forma.

En pgAdmin: actualizar Databases y abrir sistema_turnos → Schemas → public →
Tables. Hibernate crea usuarios, servicios, funcionarios y turnos al arrancar.
Registrar datos desde la web, detener, iniciar de nuevo y comprobar que siguen.

Alternativa manual en Query Tool de pgAdmin, conectado a la base postgres y con
autocommit activado: ejecutar CREATE DATABASE sistema_turnos OWNER postgres;
Después iniciar el script, que detectará la base existente.

Esta etapa utiliza ddl-auto=update para desarrollo local. No borra tablas al
reiniciar; para un despliegue posterior se deben introducir migraciones
versionadas y un usuario de aplicación con permisos limitados.

Los números de turno dependen del identificador generado en la base y no se
reinician con la aplicación. PostgreSQL añade índices únicos para impedir
documentos y servicios duplicados sin distinguir mayúsculas, dos turnos activos
del mismo usuario/servicio y dos asignaciones simultáneas al mismo funcionario.

Pruebas: .\gradlew.bat clean test acceptanceTest. Utilizan H2 temporal,
exclusivamente en el perfil test y classpath de pruebas, sin contraseña ni acceso
a la base real. No verifican los índices parciales propios de PostgreSQL.

La nueva tabla cuentas guarda los accesos y hashes de contraseña del sistema.
Consultar LOGIN.md para crear el primer administrador y cuentas de funcionarios.
El usuario de conexión postgres pertenece al servidor de base de datos.

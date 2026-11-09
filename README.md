# Sistema de Turnos de Atención

Aplicación web para gestionar usuarios, servicios, funcionarios y turnos de atención. Proyecto de Calidad de Software.

## Funcionalidades

- Inicio y cierre de sesión con roles de administrador, funcionario y usuario.
- Registro de usuarios, servicios y funcionarios.
- Solicitud, asignación, atención y cancelación de turnos.
- Atención prioritaria y control de disponibilidad de funcionarios.
- Persistencia en PostgreSQL y validación de permisos en el servidor.

## Tecnologías

Java 17, Spring Boot, Spring Security, JPA, PostgreSQL, Gradle y frontend HTML/CSS/JavaScript. Las pruebas incluyen JUnit, Cucumber, Serenity y Selenium; los entornos de pruebas utilizan H2.

## Ejecución local

Requisitos: JDK compatible con el wrapper Gradle incluido, Java 17 disponible para la aplicación y PostgreSQL.

Configuración de la conexión local en Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\configurar-conexion.ps1
```

Inicio de la aplicación desde la raíz:

```powershell
.\gradlew.bat bootRun
```

Dirección: `http://localhost:8080`.

La configuración de la base y la creación del administrador se describen en [BASE-DE-DATOS.md](BASE-DE-DATOS.md) y [LOGIN.md](LOGIN.md).

## Pruebas

Pruebas del servidor y de aceptación:

```powershell
.\gradlew.bat test acceptanceTest
```

Pruebas E2E: iniciar el servidor temporal en una terminal y ejecutar la suite en otra.

```powershell
.\gradlew.bat bootRunE2e
```

```powershell
.\gradlew.bat -p e2e clean test
```

El entorno E2E utiliza `http://127.0.0.1:8081` y una base H2 en memoria. El reporte se genera en `e2e/build/reports/cucumber.html`.

## Estructura

| Ruta | Contenido |
|---|---|
| `src/main/java` | API, seguridad y lógica de negocio |
| `src/main/resources` | Configuración, esquema SQL e interfaz web |
| `src/test` | Pruebas del servidor y escenarios de aceptación |
| `e2e` | Pruebas de navegador con Serenity y Cucumber |
| `scripts` | Configuración local y análisis de seguridad |

## Documentación

- [API y reglas de atención](API.md)
- [Base de datos](BASE-DE-DATOS.md)
- [Autenticación y permisos](LOGIN.md)
- [Configuración de pruebas E2E](e2e/README.md)
- [Análisis de seguridad con ZAP](scripts/zap/README.md)

El análisis de Sonar utiliza la variable de entorno `SONAR_TOKEN` para la autenticación.

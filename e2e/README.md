# Pruebas E2E

Suite del Sistema de Turnos con Serenity 3.9.8, Cucumber 7, Selenium y JUnit 4. Se ejecuta como proyecto Gradle independiente para aislar sus dependencias de las del servidor.

## Escenarios

| Archivo | Cobertura |
|---|---|
| `login.feature` | Inicio de sesión, identidad y rol, cierre de sesión y protección del panel |
| `login.feature` | Rechazo de contraseña incorrecta |
| `atencion_turno.feature` | Registro de servicio, usuario y funcionario; creación y atención completa de un turno |

El flujo de atención comprueba los estados GENERADO → EN_ESPERA → LLAMADO → EN_ATENCION → FINALIZADO. Al recargar la página se verifican el estado final y el funcionario asignado.

Las operaciones se realizan mediante la interfaz y el servidor reales. Cada ejecución genera nombres únicos para los registros.

## Entorno

- Chrome instalado.
- Java compatible con el wrapper Gradle del repositorio.
- Acceso a internet para descargar dependencias y resolver ChromeDriver.
- Servidor E2E disponible en `http://127.0.0.1:8081`.

El perfil `e2e` utiliza H2 en memoria y escucha únicamente en la interfaz local. Los datos se eliminan al detener el servidor. La suite no valida las restricciones específicas de PostgreSQL.

Cuenta exclusiva del entorno temporal:

| Usuario | Contraseña |
|---|---|
| `admin.e2e` | `DemoTurnos2026!` |

WebDriverManager resuelve el controlador al iniciar la suite. La descarga automática de Serenity está desactivada para evitar conflictos de versiones.

## Ejecución

Todos los comandos se ejecutan desde la raíz del repositorio.

Terminal del servidor:

```powershell
.\gradlew.bat bootRunE2e --console=plain
```

Una vez que el servidor indique `Started SistemaTurnosApplication`, ejecutar en otra terminal:

```powershell
.\gradlew.bat -p e2e clean test --console=plain
```

Ejecución sin ventana del navegador:

```powershell
.\gradlew.bat -p e2e '-Dheadless.mode=true' test --console=plain
```

El servidor se detiene con `Ctrl+C` en su terminal.

## Reportes

| Ruta desde la raíz | Contenido |
|---|---|
| `e2e/build/reports/cucumber.html` | Escenarios, resultados y capturas finales disponibles |
| `e2e/build/reports/cucumber.json` | Resultados de Cucumber en JSON |
| `e2e/build/reports/tests/test/index.html` | Informe de Gradle |
| `e2e/build/test-results/test` | Resultados XML |
| `e2e/target/site/serenity` | Resultados de Serenity, sin agregación HTML configurada |

```powershell
Start-Process .\e2e\build\reports\cucumber.html
```

La tarea `clean` elimina los reportes anteriores de `e2e/build`.

## Organización

- `src/test/resources/features`: escenarios Gherkin en español.
- `definitions`: correspondencia entre pasos Gherkin y métodos Java; preparación del controlador y capturas.
- `steps`: acciones de negocio registradas por Serenity.
- `pages`: selectores, interacciones, esperas y aserciones.
- `runners`: ejecución con el runner de Cucumber y el plugin `SerenityReporter`.
- `src/test/resources/serenity.conf`: configuración del navegador.

Las esperas dependen del estado de la interfaz. Antes de ejecutar transiciones del turno se espera a que desaparezca el aviso de éxito, que puede superponerse a los botones.

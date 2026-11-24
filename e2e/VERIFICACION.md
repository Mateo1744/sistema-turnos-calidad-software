# Ejecución verificada

Fecha: 8 de noviembre de 2026).

Se ejecutó la aplicación con `bootRunE2e` (Spring Boot real, base H2 temporal,
puerto 8081) y la suite con `gradlew.bat -p e2e clean test --console=plain`.
Navegador: Chrome 155.0.8059.85; ChromeDriver 155.0.8059.84.

| Escenario | Resultado |
|---|---|
| Crear un turno y completar todos los estados de atención | Aprobado |
| Iniciar sesión y cerrar el acceso al panel | Aprobado |
| Rechazar una contraseña incorrecta | Aprobado |

Resultado Gradle: `BUILD SUCCESSFUL`. Total: 3 pruebas, 0 fallos, 0 errores,
0 omitidas. Se revisaron los resultados XML generados por Gradle.

Reporte HTML: `build/reports/cucumber.html` (relativo a esta carpeta).
Resultados XML: `build/test-results/test/`.

Se corrigieron durante la ejecución la selección de un ChromeDriver antiguo
y la espera del aviso de éxito que tapaba un botón de la fila del turno.

Alcance: interfaz y servidor reales con H2. No se ejecutó esta suite contra
PostgreSQL. La grabación de pantalla para entregar al profesor queda pendiente;
la guía README explica cómo hacerla.

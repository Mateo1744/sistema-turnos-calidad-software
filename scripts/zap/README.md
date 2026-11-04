# Revision autenticada de laboratorio

Esta ampliacion de ET0202 conserva `azure-pipelines-zap.yml`. El archivo nuevo
`azure-pipelines-zap-autenticado.yml` ejecuta las pruebas, construye el JAR,
inicia una instancia H2 desechable, ejecuta el Baseline anonimo de la guia,
prepara datos ficticios y consulta rutas autenticadas a traves del proxy ZAP.
Al terminar publica las evidencias y detiene su instancia.

## Datos y autenticacion

Cada ejecucion genera contrasenas aleatorias en memoria. No requiere cuentas
reales, contrasenas en Azure ni secretos versionados para esta base H2.
Se crean un administrador, dos usuarios, dos funcionarios con cuenta,
dos servicios y dos turnos asignados en servicios distintos.

Las escrituras de preparacion no pasan por ZAP ni cuentan como cobertura del
analisis. El login usa un formulario, token CSRF y cookie de sesion. Antes de
consultar rutas se verifica la identidad y el rol con `/api/auth/me`.
Las sesiones se conservan solo durante esta ejecucion. No se persiste H2.

## Alcance

El programa envia una lista explicita de consultas GET a traves de ZAP.
No ejecuta un spider ni un escaneo activo. ZAP analiza pasivamente sus
respuestas. La cobertura dirigida es adecuada para esta API con rutas que
no aparecen como enlaces HTML; no implica que se hayan descubierto todas
las rutas de la aplicacion ni probado todas sus vulnerabilidades.

- Administrador: usuarios, servicios, funcionarios, turnos, filtros,
  busqueda por ID/numero, pendientes, turnos de funcionario y cuentas.
- Usuario: perfil, servicios y turnos propios; rechazos de rutas administrativas.
- Funcionario: perfil, servicios, turnos propios de su servicio y datos propios;
  rechazo de IDs de otro servicio/funcionario y rutas administrativas.
- Anonimo: confirma que las rutas protegidas responden 401.

Se consulta tambien la interfaz HTML y sus recursos. No se simulan clics ni
se prueban las operaciones de escritura durante la fase de analisis.
Los rechazos esperados (401/403) cuentan como verificaciones correctas,
no como fallos operativos. Las comprobaciones de autorizacion son controles
dirigidos adicionales; no las realiza el motor pasivo de ZAP.

## Evidencias del artefacto

`reporte-seguridad-zap-ampliado` contiene:

- `baseline/reporte-zap.html` y `.json`: Baseline de la guia, antes de sembrar datos.
- `autenticado/reporte-zap-autenticado.html` y `.json`: alertas pasivas ZAP.
- `autenticado/cobertura.csv`: rol, metodo, ruta, codigo esperado/real,
  resultado y comprobacion aplicada.
- `autenticado/alcance.json`: metodo, limites, identificador de build,
  commit, enlace de ejecucion y cobertura. El JSON de ZAP tambien lo incluye.
- Registros de la aplicacion copiados despues de detenerla.
- `autenticado/error.json`, si falla la preparacion, autenticacion o cobertura.

Las credenciales, cookies y tokens conocidos se redactan al exportar reportes.
Un error operativo, rol incorrecto, datos no esperados o respuesta distinta
a la prevista hace fallar la tarea; encontrar alertas pasivas no la bloquea.

## Ejecutar en Azure

1. Publicar el nuevo YAML y toda la carpeta `scripts/zap` en Azure Repos.
2. Crear otro pipeline con Existing Azure Pipelines YAML file y seleccionar
   `/azure-pipelines-zap-autenticado.yml` en `main`.
3. Mantener el agente de LocalPool Online y Docker Desktop con motor Linux.
4. Cerrar la instancia manual en 8081 antes de ejecutar; el pipeline levanta la suya.
5. Ejecutar y descargar el artefacto de esa ejecucion.

El pipeline anterior sigue disponible como evidencia base de la guia.
Para el informe comparar ambos reportes teniendo en cuenta el alcance y las
reglas ZAP cargadas; el numero de alertas no mide por si solo la calidad.

## Comprobacion local de la ampliacion

Despues de compilar el JAR, PowerShell permite usar un puerto independiente:

```powershell
.\scripts\zap\Invoke-ZapAuthenticated.ps1 -AppPort 8087 -ReportDirectory "$PWD\build\zap-validation" -SkipBaseline
```

`-SkipBaseline` evita repetir el Baseline al validar solo el nuevo flujo.
El pipeline de Azure no utiliza ese interruptor: ejecuta ambas partes.
La limpieza detiene exclusivamente la instancia Java y el contenedor creados
por ese script; nunca detiene otra aplicacion en caso de puerto ocupado.

## Referencias

- [ZAP Baseline](https://www.zaproxy.org/docs/docker/baseline-scan/)
- [API oficial de ZAP](https://www.zaproxy.org/docs/api/)
- [Analisis pasivo](https://www.zaproxy.org/docs/desktop/start/features/pscan/)

param(
    [ValidateRange(1024, 65535)][int]$AppPort = 8081,
    [Parameter(Mandatory = $true)][string]$ReportDirectory,
    [switch]$SkipBaseline
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$reportRoot = [IO.Path]::GetFullPath($ReportDirectory)
$runId = [Guid]::NewGuid().ToString('N')
$containerName = "zap-lab-$runId"
$temporaryLogs = Join-Path ([IO.Path]::GetTempPath()) "zap-lab-$runId"
$app = $null
$originalEnvironment = @{}
$environmentNames = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'ADMIN_USERNAME', 'ADMIN_PASSWORD',
    'LAB_ADMIN_USERNAME', 'LAB_ADMIN_PASSWORD', 'ZAP_TARGET', 'BUILD_ID', 'BUILD_COMMIT', 'BUILD_URL')
foreach ($name in $environmentNames) {
    $originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    New-Item -ItemType Directory -Force $reportRoot, $temporaryLogs | Out-Null
    $publicDir = Join-Path $reportRoot 'baseline'
    $authDir = Join-Path $reportRoot 'autenticado'
    New-Item -ItemType Directory -Force $publicDir, $authDir | Out-Null
    if (Get-NetTCPConnection -State Listen -LocalPort $AppPort -ErrorAction SilentlyContinue) {
        throw "El puerto $AppPort esta ocupado. Cierra tu instancia local o elige otro puerto."
    }
    $jar = Get-ChildItem (Join-Path $root 'build\libs') -Filter '*.jar' |
        Where-Object { $_.Name -notlike '*-plain.jar' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { throw 'Falta bootJar. Ejecuta gradlew.bat test bootJar primero.' }
    docker info --format '{{.OSType}}'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo acceder al motor Docker.' }
    $env:DB_URL = 'jdbc:h2:mem:zapdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1'
    $env:DB_USERNAME = 'sa'
    $env:DB_PASSWORD = ''
    $env:LAB_ADMIN_USERNAME = 'zapadmin-ci'
    $env:LAB_ADMIN_PASSWORD = [Guid]::NewGuid().ToString('N') + 'Aa1!'
    $env:ADMIN_USERNAME = $env:LAB_ADMIN_USERNAME
    $env:ADMIN_PASSWORD = $env:LAB_ADMIN_PASSWORD
    $env:ZAP_TARGET = "http://host.docker.internal:$AppPort"
    if (-not $env:BUILD_ID) { $env:BUILD_ID = "local-$runId" }
    $args = '-jar "' + $jar.FullName + '" --spring.profiles.active=zap --server.port=' + $AppPort +
        ' --spring.datasource.driver-class-name=org.h2.Driver --spring.sql.init.mode=never' +
        ' --spring.jpa.hibernate.ddl-auto=create-drop'
    $out = Join-Path $temporaryLogs 'application-output.log'
    $err = Join-Path $temporaryLogs 'application-error.log'
    $app = Start-Process -FilePath 'java.exe' -ArgumentList $args -WorkingDirectory $root `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput $out -RedirectStandardError $err
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $app.Refresh()
        if ($app.HasExited) { throw 'La instancia de laboratorio termino durante el arranque; revisa sus logs.' }
        try {
            $response = Invoke-WebRequest "http://127.0.0.1:$AppPort/api/auth/csrf" -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw 'La instancia de laboratorio no respondio a tiempo.' }
    Write-Host "Aplicacion aislada disponible en el puerto $AppPort."

    if (-not $SkipBaseline) {
        Write-Host 'Ejecutando Baseline anonimo de la guia.'
        docker run --rm --name $containerName -v "${publicDir}:/zap/wrk:rw" `
            ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t $env:ZAP_TARGET -m 2 `
            -r reporte-zap.html -J reporte-zap.json -I
        $baselineExit = $LASTEXITCODE
        if ($baselineExit -notin @(0, 1, 2)) { throw "Fallo operativo de Baseline: $baselineExit" }
        foreach ($file in @('reporte-zap.html', 'reporte-zap.json')) {
            if (-not (Test-Path (Join-Path $publicDir $file))) { throw "Falta $file de Baseline." }
        }
    }

    Write-Host 'Preparando datos ficticios y verificando consultas autenticadas de tres roles.'
    $scriptDir = [IO.Path]::GetFullPath($PSScriptRoot)
    docker run --rm --name $containerName -v "${scriptDir}:/zap/scripts:ro" -v "${authDir}:/zap/wrk:rw" `
        -e LAB_ADMIN_USERNAME -e LAB_ADMIN_PASSWORD -e ZAP_TARGET -e BUILD_ID -e BUILD_COMMIT -e BUILD_URL `
        ghcr.io/zaproxy/zaproxy:stable python /zap/scripts/authenticated_scan.py
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la revision autenticada. Revisa autenticado/error.json y cobertura.csv.' }
    foreach ($file in @('reporte-zap-autenticado.html', 'reporte-zap-autenticado.json', 'cobertura.csv', 'alcance.json')) {
        if (-not (Test-Path (Join-Path $authDir $file))) { throw "Falta evidencia autenticada: $file" }
    }
} finally {
    # Only stop this run's container and application, never another lab or Desktop ZAP.
    $remaining = docker container ls -aq --filter "name=^${containerName}$"
    if ($remaining) { docker rm -f $containerName | Out-Null }
    if ($app) {
        $app.Refresh()
        if (-not $app.HasExited) {
            Stop-Process -Id $app.Id -Force -ErrorAction SilentlyContinue
            $null = $app.WaitForExit(10000)
        }
    }
    # Copy only closed, sanitized application logs; publishing never touches live handles.
    foreach ($file in @('application-output.log', 'application-error.log')) {
        $logFile = Join-Path $temporaryLogs $file
        if (Test-Path $logFile) {
            try {
                $contents = [IO.File]::ReadAllText($logFile)
                if ($env:LAB_ADMIN_PASSWORD) { $contents = $contents.Replace($env:LAB_ADMIN_PASSWORD, '[REDACTED]') }
                [IO.File]::WriteAllText((Join-Path $reportRoot $file), $contents)
            } catch { Write-Warning "No se pudo copiar el registro cerrado $file." }
        }
    }
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $originalEnvironment[$name], 'Process')
    }
    $global:LASTEXITCODE = 0
}

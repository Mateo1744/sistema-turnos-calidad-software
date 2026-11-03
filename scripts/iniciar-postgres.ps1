param(
    [string]$Servidor = "localhost",
    [int]$Puerto = 5432,
    [switch]$CrearAdmin
)
$ErrorActionPreference = "Stop"
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path -LiteralPath $psql)) {
    throw "No se encontró psql. Ajusta la ruta a tu instalación de PostgreSQL."
}
$claveSegura = Read-Host "Contraseña local de postgres (no se mostrará)" -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveSegura)
$anteriorPg = $env:PGPASSWORD
$anteriorDb = $env:DB_PASSWORD
$anteriorUrl = $env:DB_URL
$anteriorUsuario = $env:DB_USERNAME
$anteriorAdminUsuario = $env:ADMIN_USERNAME
$anteriorAdminClave = $env:ADMIN_PASSWORD
$punteroAdmin = [IntPtr]::Zero
$claveAdmin = $null
try {
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $env:DB_PASSWORD = $env:PGPASSWORD
    $env:DB_USERNAME = "postgres"
    $env:DB_URL = "jdbc:postgresql://${Servidor}:${Puerto}/sistema_turnos"
    $existe = & $psql -h $Servidor -p $Puerto -U postgres -d postgres -w -At -v ON_ERROR_STOP=1 -c "SELECT 1 FROM pg_database WHERE datname = 'sistema_turnos';"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo autenticar con PostgreSQL. Revisa la contraseña y el puerto." }
    if ($existe -ne "1") {
        & $psql -h $Servidor -p $Puerto -U postgres -d postgres -w -v ON_ERROR_STOP=1 -c "CREATE DATABASE sistema_turnos OWNER postgres ENCODING 'UTF8';"
        if ($LASTEXITCODE -ne 0) { throw "No se pudo crear sistema_turnos." }
    }
    if ($CrearAdmin) {
        $env:ADMIN_USERNAME = Read-Host "Elige el nombre de usuario del administrador"
        $claveAdmin = Read-Host "Contraseña del administrador (12 a 64 caracteres)" -AsSecureString
        $punteroAdmin = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveAdmin)
        $env:ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($punteroAdmin)
        Write-Host "Se creará el administrador si todavía no existe uno. Una cuenta existente no se modifica."
    }
    Write-Host "Base sistema_turnos disponible. Iniciando la aplicación..."
    Push-Location (Split-Path -Parent $PSScriptRoot)
    try {
        & .\gradlew.bat bootRun --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "La aplicación no pudo iniciar. Revisa el mensaje anterior." }
    } finally { Pop-Location }
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    $env:PGPASSWORD = $anteriorPg
    $env:DB_PASSWORD = $anteriorDb
    $env:DB_URL = $anteriorUrl
    $env:DB_USERNAME = $anteriorUsuario
    $env:ADMIN_USERNAME = $anteriorAdminUsuario
    $env:ADMIN_PASSWORD = $anteriorAdminClave
    if ($punteroAdmin -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($punteroAdmin) }
    if ($null -ne $claveAdmin) { $claveAdmin.Dispose() }
    $claveSegura.Dispose()
}

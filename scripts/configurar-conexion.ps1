param(
    [string]$Servidor = "localhost",
    [int]$Puerto = 5432,
    [string]$Usuario = "postgres"
)
$ErrorActionPreference = "Stop"
if ($env:OS -ne "Windows_NT") { throw "Esta configuración cifrada requiere Windows." }
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path -LiteralPath $psql)) { throw "No se encontró PostgreSQL 18. Ajusta la ruta de psql." }
$secreto = Read-Host "Contraseña de PostgreSQL para $Usuario (se guardará cifrada solo en este equipo)" -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secreto)
$anterior = $env:PGPASSWORD
try {
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    & $psql -h $Servidor -p $Puerto -U $Usuario -d sistema_turnos -w -v ON_ERROR_STOP=1 -c "SELECT 1;" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "No se pudo conectar a sistema_turnos. No se guardó la configuración." }
    $directorio = Join-Path (Split-Path -Parent $PSScriptRoot) ".local"
    New-Item -ItemType Directory -Path $directorio -Force | Out-Null
    [pscustomobject]@{
        Servidor = $Servidor
        Puerto = $Puerto
        Credencial = [pscredential]::new($Usuario, $secreto)
    } | Export-Clixml -LiteralPath (Join-Path $directorio "postgres.clixml") -Force
    Write-Host "Conexión validada y guardada con cifrado de Windows. Ya puedes usar .\gradlew.bat bootRun"
} finally {
    $env:PGPASSWORD = $anterior
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    $secreto.Dispose()
}

param(
    [string]$Archivo = (Join-Path (Split-Path -Parent $PSScriptRoot) ".local\postgres.clixml")
)
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
try {
    if ($env:OS -ne "Windows_NT") { throw "Se requiere Windows." }
    $conexion = Import-Clixml -LiteralPath $Archivo
    if ($conexion.Credencial -isnot [pscredential]) { throw "Credencial no válida." }
    [ordered]@{
        DB_URL = "jdbc:postgresql://$($conexion.Servidor):$($conexion.Puerto)/sistema_turnos"
        DB_USERNAME = $conexion.Credencial.UserName
        DB_PASSWORD = $conexion.Credencial.GetNetworkCredential().Password
    } | ConvertTo-Json -Compress
} catch {
    # No incluir datos descifrados en errores ni registros de Gradle.
    [Console]::Error.WriteLine("No se pudo descifrar la conexión con este usuario de Windows.")
    exit 1
}

$ErrorActionPreference = "Stop"
Write-Host "Esta herramienta cambia la contraseña de un ADMIN existente. No elimina cuentas ni datos."
$usuarioAdmin = Read-Host "Nombre de usuario del administrador (el username que viste en pgAdmin)"
$clave = Read-Host "Nueva contraseña (12 a 64 caracteres)" -AsSecureString
$confirmacion = Read-Host "Repite la nueva contraseña" -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave)
$punteroConfirmacion = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirmacion)
$anteriorUsuario = $env:RESET_ADMIN_USERNAME
$anteriorClave = $env:RESET_ADMIN_PASSWORD
try {
    $nueva = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    if ($nueva -cne [Runtime.InteropServices.Marshal]::PtrToStringBSTR($punteroConfirmacion)) {
        throw "Las contraseñas no coinciden. No se realizó ningún cambio."
    }
    if ($nueva.Length -lt 12 -or $nueva.Length -gt 64 -or [Text.Encoding]::UTF8.GetByteCount($nueva) -gt 72) {
        throw "Usa de 12 a 64 caracteres y como máximo 72 bytes UTF-8. No se realizó ningún cambio."
    }
    $env:RESET_ADMIN_USERNAME = $usuarioAdmin.Trim()
    $env:RESET_ADMIN_PASSWORD = $nueva
    Push-Location (Split-Path -Parent $PSScriptRoot)
    try {
        & .\gradlew.bat restablecerAdmin --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "No se pudo restablecer la contraseña. Revisa el error anterior." }
    } finally { Pop-Location }
} finally {
    $env:RESET_ADMIN_USERNAME = $anteriorUsuario
    $env:RESET_ADMIN_PASSWORD = $anteriorClave
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($punteroConfirmacion)
    $clave.Dispose()
    $confirmacion.Dispose()
    $nueva = $null
}

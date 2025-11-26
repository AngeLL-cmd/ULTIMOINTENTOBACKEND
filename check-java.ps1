# Script para verificar y configurar Java para el proyecto
Write-Host "=== Verificador de Java para Sistema Electoral Backend ===" -ForegroundColor Cyan
Write-Host ""

# Verificar versión actual de Java
Write-Host "Versión de Java actual:" -ForegroundColor Yellow
$javaVersion = java -version 2>&1 | Select-Object -First 1
Write-Host $javaVersion

# Verificar si es Java 25
if ($javaVersion -match "version.*25") {
    Write-Host ""
    Write-Host "⚠️  ADVERTENCIA: Estás usando Java 25, que puede causar problemas de compilación." -ForegroundColor Red
    Write-Host "Se recomienda usar Java 21 (LTS) o Java 17." -ForegroundColor Yellow
    Write-Host ""
}

# Buscar instalaciones de Java
Write-Host "Buscando instalaciones de Java en el sistema..." -ForegroundColor Cyan
$javaPaths = @()

# Buscar en ubicaciones comunes
$commonPaths = @(
    "C:\Program Files\Java",
    "C:\Program Files (x86)\Java",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Microsoft",
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Eclipse Adoptium"
)

foreach ($path in $commonPaths) {
    if (Test-Path $path) {
        $jdkDirs = Get-ChildItem -Path $path -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "jdk|java" }
        foreach ($dir in $jdkDirs) {
            $javaExe = Join-Path $dir.FullName "bin\java.exe"
            if (Test-Path $javaExe) {
                $version = & $javaExe -version 2>&1 | Select-Object -First 1
                $javaPaths += [PSCustomObject]@{
                    Path = $dir.FullName
                    Version = $version
                }
            }
        }
    }
}

if ($javaPaths.Count -gt 0) {
    Write-Host ""
    Write-Host "Instalaciones de Java encontradas:" -ForegroundColor Green
    $index = 1
    foreach ($java in $javaPaths) {
        Write-Host "$index. $($java.Path)" -ForegroundColor White
        Write-Host "   $($java.Version)" -ForegroundColor Gray
        $index++
    }
    
    Write-Host ""
    Write-Host "Para usar una versión específica de Java, ejecuta:" -ForegroundColor Yellow
    Write-Host '  $env:JAVA_HOME = "RUTA_DE_JAVA"' -ForegroundColor Cyan
    Write-Host '  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Ejemplo para usar Java 21:" -ForegroundColor Yellow
    Write-Host '  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"' -ForegroundColor Cyan
    Write-Host '  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"' -ForegroundColor Cyan
    Write-Host '  java -version' -ForegroundColor Cyan
    Write-Host '  mvn spring-boot:run' -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "No se encontraron instalaciones de Java en las ubicaciones comunes." -ForegroundColor Yellow
    Write-Host "Descarga Java 21 (LTS) desde: https://adoptium.net/" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "=== Fin del verificador ===" -ForegroundColor Cyan


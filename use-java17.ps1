# Script para configurar Java 17 para esta sesión
Write-Host "Configurando Java 17 para esta sesión..." -ForegroundColor Cyan

$java17Path = "C:\Program Files\Java\jdk-17"

if (Test-Path $java17Path) {
    $env:JAVA_HOME = $java17Path
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    
    Write-Host "✓ JAVA_HOME configurado a: $env:JAVA_HOME" -ForegroundColor Green
    Write-Host ""
    Write-Host "Verificando versión de Java:" -ForegroundColor Yellow
    java -version
    Write-Host ""
    Write-Host "✓ Java 17 está listo para usar. Ahora puedes ejecutar:" -ForegroundColor Green
    Write-Host "  mvn clean" -ForegroundColor Cyan
    Write-Host "  mvn spring-boot:run" -ForegroundColor Cyan
} else {
    Write-Host "✗ Error: No se encontró Java 17 en $java17Path" -ForegroundColor Red
    Write-Host "Verifica la ruta de instalación de Java 17." -ForegroundColor Yellow
}


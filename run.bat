@echo off
REM Script para ejecutar el proyecto con Java 17
echo Configurando Java 17...
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo Verificando versión de Java:
java -version

echo.
echo Ejecutando Spring Boot...
mvn spring-boot:run


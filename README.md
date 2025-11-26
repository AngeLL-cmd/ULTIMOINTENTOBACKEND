# Sistema Electoral Backend

Backend en Java Spring Boot para el Sistema Electoral Perú 2025.

## Requisitos

- **Java 21 (LTS) recomendado** o Java 17 (Java 25 puede causar problemas de compatibilidad)
- Maven 3.6+
- Cuenta de Supabase (proyecto creado)
- FastAPI corriendo en `http://localhost:8000`

> **⚠️ Nota importante:** Si tienes Java 25 instalado y encuentras errores de compilación como `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`, necesitas usar Java 21 (LTS) en su lugar. Ver la sección de solución de problemas más abajo.

## Verificar Instalación

Abre CMD y verifica que tienes Java y Maven instalados:

```cmd
java -version
mvn -version
```

Si no tienes Maven instalado, descárgalo de: https://maven.apache.org/download.cgi

## Configuración

### 1. Configurar Supabase

1. Crea un proyecto en [Supabase](https://supabase.com)
2. Obtén tu URL y API keys desde Settings > API

### 2. Configurar application.properties

Edita el archivo `src\main\resources\application.properties`:

```properties
# Supabase Configuration
supabase.url=https://tu-proyecto.supabase.co
supabase.key=tu-anon-key-aqui
supabase.service-key=tu-service-role-key-aqui

# FastAPI
fastapi.base-url=http://localhost:8000
fastapi.reniec-endpoint=/api/reniec
```

## Ejecutar el Proyecto

### Opción 1: Usar el script automático (RECOMENDADO)

Si tienes Java 25 instalado pero también tienes Java 17, usa el script `run.bat`:

```cmd
cd backend
run.bat
```

Este script configura automáticamente Java 17 y ejecuta la aplicación.

### Opción 2: Configurar Java 17 manualmente

Abre CMD o PowerShell en la carpeta `backend`:

**En CMD:**
```cmd
cd backend
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
mvn spring-boot:run
```

**En PowerShell:**
```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

### Opción 3: Si solo tienes Java 21 o superior compatible

```cmd
cd backend
mvn clean install
mvn spring-boot:run
```

El servidor estará disponible en `http://localhost:8080`

## Solución de Problemas

### Error: "mvn no se reconoce como comando"
- Asegúrate de tener Maven instalado y en el PATH

### Error de conexión a Supabase
- Verifica que la URL de Supabase sea correcta
- Verifica que las API keys sean válidas
- Verifica que las tablas existan en tu proyecto de Supabase

### Error de conexión a FastAPI
- Verifica que FastAPI esté corriendo en `http://localhost:8000`
- Verifica la URL en `application.properties`

### Puerto 8080 ya en uso
- Cambia el puerto en `application.properties`:
  ```properties
  server.port=8081
  ```

### Error: `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`
Este error ocurre cuando usas Java 25 (o versiones muy recientes) debido a incompatibilidades con el compilador de Maven.

**Solución 1: Usar Java 21 (LTS) - RECOMENDADO**

1. Descarga Java 21 LTS desde: https://adoptium.net/ (Eclipse Temurin)
2. Instala Java 21 en una ruta como `C:\Program Files\Java\jdk-21`
3. Configura JAVA_HOME para usar Java 21:
   ```cmd
   setx JAVA_HOME "C:\Program Files\Java\jdk-21"
   setx PATH "%JAVA_HOME%\bin;%PATH%"
   ```
4. Cierra y vuelve a abrir la terminal
5. Verifica la versión:
   ```cmd
   java -version
   ```
   Debería mostrar "openjdk version 21.x.x"

**Solución 2: Configurar JAVA_HOME temporalmente (solo para esta sesión)**

Si tienes Java 21 instalado pero Maven está usando Java 25:

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
java -version
mvn spring-boot:run
```

**Solución 3: Usar Java 17**

Si prefieres usar Java 17, sigue los mismos pasos pero con la ruta de Java 17.

## Despliegue en Railway

### Configuración de Variables de Entorno

En Railway, configura las siguientes variables de entorno en la sección **Variables**:

```bash
# CORS - IMPORTANTE: Incluye TODOS los dominios de Vercel con https://
CORS_ALLOWED_ORIGINS=https://frontend-elecciones1.vercel.app,https://frontend-elecciones1-git-main-elmatahansel-6591s-projects.vercel.app,https://frontend-elecciones1-p7ycduzh8-elmatahansel-6591s-projects.vercel.app

# Factiliza API
FACTILIZA_API_KEY=tu-api-key-aqui
FACTILIZA_BASE_URL=https://api.factiliza.com
FACTILIZA_ENDPOINT=/v1/dni/info
FACTILIZA_TIMEOUT=30000

# Java Options
JAVA_OPTS=-Xmx512m -Xms256m

# JWT
JWT_EXPIRATION=86400000
JWT_SECRET=tu-jwt-secret-aqui

# Puerto (Railway lo asigna automáticamente, pero puedes especificarlo)
PORT=8080

# Supabase
SUPABASE_KEY=tu-supabase-anon-key
SUPABASE_SERVICE_KEY=tu-supabase-service-key
SUPABASE_URL=https://tu-proyecto.supabase.co
```

### ⚠️ Puntos Importantes para CORS

1. **Protocolo HTTPS**: Todos los dominios deben incluir `https://` al inicio
2. **Múltiples dominios**: Separa los dominios con comas (sin espacios)
3. **Todos los dominios de Vercel**: Incluye:
   - El dominio principal: `https://frontend-elecciones1.vercel.app`
   - Los dominios de preview: `https://frontend-elecciones1-git-main-...`
   - Los dominios de deployment: `https://frontend-elecciones1-p7ycduzh8-...`

### Configuración del Frontend en Vercel

En Vercel, configura la variable de entorno:

```bash
VITE_API_URL=https://tu-backend.up.railway.app/api
```

**⚠️ IMPORTANTE**: Asegúrate de incluir `/api` al final de la URL, ya que todas las rutas del backend tienen el prefijo `/api`.

### Verificar la Conexión

1. Despliega el backend en Railway
2. Obtén la URL pública (ej: `https://bk-production-d49c.up.railway.app`)
3. Verifica que el endpoint de health funcione:
   ```
   https://bk-production-d49c.up.railway.app/api/health
   ```
4. Configura `VITE_API_URL` en Vercel con la URL completa incluyendo `/api`
5. Despliega el frontend en Vercel
6. Abre la consola del navegador para verificar que no haya errores de CORS

### Solución de Problemas de CORS

Si sigues teniendo problemas de CORS:

1. **Verifica que todos los dominios estén en CORS_ALLOWED_ORIGINS** con `https://`
2. **Verifica que la URL del frontend incluya `/api`** al final
3. **Revisa los logs de Railway** para ver errores específicos
4. **Abre la consola del navegador** (F12) y revisa los errores de red
5. **Asegúrate de que no haya espacios** en la variable `CORS_ALLOWED_ORIGINS`
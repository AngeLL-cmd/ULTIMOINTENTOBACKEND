# Guía de Deploy - Backend Sistema Electoral (Sin Docker)

Esta guía te ayudará a desplegar el backend en **Railway** o **Render** usando Maven directamente, sin Docker.

## 📋 Requisitos Previos

1. Cuenta en [Railway](https://railway.app) o [Render](https://render.com)
2. Repositorio Git (GitHub, GitLab, etc.)
3. Credenciales de Supabase y Factiliza configuradas

---

## 🚂 Deploy en Railway (Sin Docker)

### Paso 1: Preparar el Repositorio

1. Asegúrate de que el código esté en un repositorio Git
2. Verifica que el `pom.xml` esté en la carpeta `backend/`
3. El archivo `railway.json` ya está configurado para usar Nixpacks (build automático)

### Paso 2: Crear Proyecto en Railway

1. Ve a [railway.app](https://railway.app) e inicia sesión
2. Haz clic en **"New Project"**
3. Selecciona **"Deploy from GitHub repo"** (o tu proveedor Git)
4. Selecciona tu repositorio
5. Railway detectará automáticamente que es un proyecto Java/Maven

### Paso 3: Configurar Root Directory

1. Ve a **Settings** → **Root Directory**
2. Establece: `backend`
3. Esto le dice a Railway dónde está el `pom.xml`

### Paso 4: Configurar Variables de Entorno

En Railway, ve a tu servicio → **Variables** y agrega:

```env
# Puerto (Railway lo asigna automáticamente)
PORT=8080

# Java Options (opcional, para optimizar memoria)
JAVA_OPTS=-Xmx512m -Xms256m

# Supabase
SUPABASE_URL=https://dlobxwdgyrhoaochrout.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRsb2J4d2RneXJob2FvY2hyb3V0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwOTMxNzYsImV4cCI6MjA3ODY2OTE3Nn0.XSoBqCO-qZg27eWabdIPpr1ohKbAr_RXUb9ZGhYtfNA
SUPABASE_SERVICE_KEY=sb_secret_KX2Y2w0KG2sFAj7ypG_Plw_dzEnyXl9

# Factiliza API
FACTILIZA_BASE_URL=https://api.factiliza.com
FACTILIZA_ENDPOINT=/v1/dni/info
FACTILIZA_API_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIzOTg5OCIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vd3MvMjAwOC8wNi9pZGVudGl0eS9jbGFpbXMvcm9sZSI6ImNvbnN1bHRvciJ9.AlBYZrmEWMWPKTYOP0Zxj0_BMEAlZ8FsqD0qTMJeDwI
FACTILIZA_TIMEOUT=30000

# JWT
JWT_SECRET=cCiYPBmJKHQemZKcVT/lHiHOwG6bzxMNLYd1Y0+XoXcNWKaOKM048p1kU9C7LUBfJq7rDz0lRYOSUzqckNceVg==
JWT_EXPIRATION=86400000

# CORS (IMPORTANTE: Reemplaza con la URL de tu frontend)
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app,https://tu-frontend.netlify.app

# Admin
ADMIN_EMAIL=admin@elecciones.pe
ADMIN_PASSWORD=admin123

# Logging (opcional)
LOG_LEVEL_APP=INFO
LOG_LEVEL_SPRING=INFO
LOG_LEVEL_HIBERNATE=INFO
```

### Paso 5: Configurar Build y Start Commands

Railway debería detectar automáticamente los comandos, pero puedes verificarlos en **Settings**:

- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/sistema-electoral-backend-1.0.0.jar`

### Paso 6: Deploy

1. Railway comenzará a construir automáticamente
2. Espera a que termine el build (puede tardar 5-10 minutos la primera vez)
3. Tu aplicación estará disponible en: `https://tu-proyecto.up.railway.app`

### Paso 7: Verificar

```bash
curl https://tu-proyecto.up.railway.app/api/health
```

---

## 🎨 Deploy en Render (Sin Docker)

### Paso 1: Preparar el Repositorio

1. Asegúrate de que el código esté en un repositorio Git
2. Verifica que el `pom.xml` esté en la carpeta `backend/`
3. El archivo `render.yaml` ya está configurado

### Paso 2: Crear Servicio en Render

1. Ve a [render.com](https://render.com) e inicia sesión
2. Haz clic en **"New +"** → **"Blueprint"** (si usas `render.yaml`) o **"Web Service"**
3. Conecta tu repositorio Git

### Opción A: Usando render.yaml (Recomendado)

1. Render detectará automáticamente el archivo `render.yaml`
2. Haz clic en **"Apply"** para crear el servicio
3. Render usará la configuración del archivo

### Opción B: Configuración Manual

1. Selecciona **"Web Service"**
2. Configura:
   - **Name**: `sistema-electoral-backend`
   - **Environment**: `Java`
   - **Region**: Elige la región más cercana
   - **Branch**: `main` (o tu rama principal)
   - **Root Directory**: `backend`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/sistema-electoral-backend-1.0.0.jar`

### Paso 3: Configurar Variables de Entorno

En **Environment Variables**, agrega:

```env
# Puerto (Render lo asigna automáticamente)
PORT=8080

# Java Options (opcional, para optimizar memoria)
JAVA_OPTS=-Xmx512m -Xms256m

# Supabase
SUPABASE_URL=https://dlobxwdgyrhoaochrout.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRsb2J4d2RneXJob2FvY2hyb3V0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwOTMxNzYsImV4cCI6MjA3ODY2OTE3Nn0.XSoBqCO-qZg27eWabdIPpr1ohKbAr_RXUb9ZGhYtfNA
SUPABASE_SERVICE_KEY=sb_secret_KX2Y2w0KG2sFAj7ypG_Plw_dzEnyXl9

# Factiliza API
FACTILIZA_BASE_URL=https://api.factiliza.com
FACTILIZA_ENDPOINT=/v1/dni/info
FACTILIZA_API_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIzOTg5OCIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vd3MvMjAwOC8wNi9pZGVudGl0eS9jbGFpbXMvcm9sZSI6ImNvbnN1bHRvciJ9.AlBYZrmEWMWPKTYOP0Zxj0_BMEAlZ8FsqD0qTMJeDwI
FACTILIZA_TIMEOUT=30000

# JWT
JWT_SECRET=cCiYPBmJKHQemZKcVT/lHiHOwG6bzxMNLYd1Y0+XoXcNWKaOKM048p1kU9C7LUBfJq7rDz0lRYOSUzqckNceVg==
JWT_EXPIRATION=86400000

# CORS (IMPORTANTE: Reemplaza con la URL de tu frontend)
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app,https://tu-frontend.netlify.app

# Admin
ADMIN_EMAIL=admin@elecciones.pe
ADMIN_PASSWORD=admin123

# Logging (opcional)
LOG_LEVEL_APP=INFO
LOG_LEVEL_SPRING=INFO
LOG_LEVEL_HIBERNATE=INFO
```

### Paso 4: Configurar Auto-Deploy

- **Auto-Deploy**: `Yes` (para deploy automático en cada push)
- **Health Check Path**: `/api/health`

### Paso 5: Crear el Servicio

1. Haz clic en **"Create Web Service"** (o **"Apply"** si usas `render.yaml`)
2. Render comenzará a construir la aplicación con Maven
3. Espera a que termine el build (puede tardar 5-10 minutos la primera vez)

### Paso 6: Verificar

Una vez desplegado, tu aplicación estará disponible en:
`https://tu-servicio.onrender.com`

Verifica con:
```bash
curl https://tu-servicio.onrender.com/api/health
```

---

## ⚙️ Actualizar application.properties para Variables de Entorno

Para que las variables de entorno funcionen, necesitas actualizar `application.properties`:

```properties
# Server Configuration
server.port=${PORT:8080}

# Supabase Configuration
supabase.url=${SUPABASE_URL:https://dlobxwdgyrhoaochrout.supabase.co}
supabase.key=${SUPABASE_KEY}
supabase.service-key=${SUPABASE_SERVICE_KEY}

# FastAPI Configuration
fastapi.base-url=${FACTILIZA_BASE_URL:https://api.factiliza.com}
fastapi.reniec-endpoint=${FACTILIZA_ENDPOINT:/v1/dni/info}
fastapi.api-key=${FACTILIZA_API_KEY}
fastapi.timeout=${FACTILIZA_TIMEOUT:30000}

# JWT Configuration
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}

# CORS Configuration
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}
cors.allowed-methods=${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}
cors.allowed-headers=${CORS_ALLOWED_HEADERS:*}
cors.allow-credentials=${CORS_ALLOW_CREDENTIALS:true}

# Admin Configuration
admin.email=${ADMIN_EMAIL:admin@elecciones.pe}
admin.password=${ADMIN_PASSWORD:admin123}
```

**Nota**: Si prefieres mantener los valores por defecto en `application.properties` para desarrollo local, puedes crear un `application-prod.properties` separado.

---

## 🔧 Configuración del Frontend

Después de desplegar el backend, actualiza tu frontend para usar la nueva URL:

### En `vite.config.ts`:

```typescript
proxy: {
  "/api": {
    target: "https://tu-backend.up.railway.app", // o tu URL de Render
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, "/api"),
  },
}
```

### O directamente en `src/services/api.ts`:

```typescript
const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://tu-backend.up.railway.app';
```

Y agrega en tu `.env`:
```env
VITE_API_URL=https://tu-backend.up.railway.app
```

---

## ⚠️ Notas Importantes

### CORS

**IMPORTANTE**: Asegúrate de actualizar `CORS_ALLOWED_ORIGINS` con la URL real de tu frontend desplegado. Si no lo haces, el frontend no podrá hacer peticiones al backend.

### Puerto

Tanto Railway como Render asignan automáticamente un puerto mediante la variable `PORT`. El código ya está configurado para usar esta variable.

### Java Version

Railway y Render usarán Java 17 automáticamente basándose en el `pom.xml`.

### Logs

Puedes ver los logs en tiempo real desde el dashboard de Railway o Render.

### Actualizaciones

Con auto-deploy habilitado, cada push a la rama principal desplegará automáticamente los cambios.

---

## 🐛 Troubleshooting

### Error: "Port already in use"
- Verifica que estés usando la variable `PORT` en `application.properties`
- Asegúrate de que `server.port=${PORT:8080}` esté configurado

### Error: "Connection refused" desde el frontend
- Verifica que `CORS_ALLOWED_ORIGINS` incluya la URL de tu frontend
- Verifica que el backend esté corriendo (revisa los logs)

### Error: "Build failed"
- Verifica que el `pom.xml` esté correcto
- Revisa los logs del build para más detalles
- Asegúrate de que Maven pueda descargar las dependencias

### Error: "Supabase connection failed"
- Verifica que las credenciales de Supabase sean correctas
- Verifica que las variables de entorno estén configuradas

### Error: "Java not found"
- Railway y Render deberían detectar automáticamente Java 17
- Si hay problemas, verifica que el `pom.xml` especifique Java 17

---

## 📚 Recursos Adicionales

- [Railway Documentation](https://docs.railway.app)
- [Render Documentation](https://render.com/docs)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)


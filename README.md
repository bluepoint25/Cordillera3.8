# Cordillera3.8 - Plataforma de Microservicios

Una plataforma integral basada en microservicios construida con Spring Boot 3.3.5 y Spring Cloud, con un API Gateway, múltiples microservicios especializados, un frontend React e infraestructura de base de datos MySQL.

## Arquitectura del Proyecto

### 📦 Módulos

#### **API Gateway** (`api-gateway/`)
- Spring Cloud Gateway MVC para enrutamiento de solicitudes y equilibrio de carga
- Seguridad y validación de tokens JWT
- Patrón de Circuit Breaker (Resilience4j)
- Configuración de servidor de recursos OAuth2
- Puerto: 8080 (predeterminado)

#### **Microservicios**

1. **MS-Login** (`MS-login/`)
   - Autenticación de usuarios y gestión de sesiones
   - Generación de tokens JWT
   - Flujos de registro e inicio de sesión
   - Base de datos MySQL (db_login)

2. **MS-Data** (`MS-data/`)
   - Microservicio central de gestión de datos
   - ORM JPA/Hibernate para persistencia de datos
   - Integración de cola de mensajes Kafka
   - Base de datos MySQL (ms_datos, db_data)

3. **MS-KPI** (`MS-kpi/`)
   - Seguimiento de indicadores clave de desempeño (KPI)
   - Agregación de datos de análisis e informes
   - Base de datos MySQL (ms_kpi, db_kpi)

4. **MS-Reportes** (`MS-reportes/`)
   - Generación y gestión de reportes
   - Funcionalidad de exportación de datos
   - Base de datos MySQL (ms_reportes, db_reportes)

#### **Frontend** (`frontend/`)
- Aplicación de página única (SPA) React + Vite
- Gestión de estado con Context API
- Rutas protegidas con control de acceso basado en roles
- Componentes: Navegación, Autenticación, Dashboard de Usuario/Admin/Vendedor
- Proxy inverso Nginx en Docker

## Stack Tecnológico

- **Framework Backend**: Spring Boot 3.3.5
- **Versión Java**: 17 (compatible con Java 21)
- **Herramienta de Compilación**: Maven 3.x con Maven Wrapper (mvnw)
- **Spring Cloud**: 2023.0.3
- **Cola de Mensajes**: Apache Kafka
- **Base de Datos**: MySQL 5.7+
- **ORM**: Spring Data JPA / Hibernate
- **Seguridad**: Spring Security + OAuth2
- **Frontend**: React 18+, Vite, TailwindCSS
- **Contenedorización**: Docker & Docker Compose
- **Servidor Web**: Nginx

## Requisitos Previos

- Java 17+ (o Java 21 para el LTS más reciente)
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (para desarrollo del frontend)
- MySQL 5.7+ (si se ejecuta sin Docker)

## Inicio Rápido

### Usando Docker Compose (Recomendado)

```bash
# Iniciar todos los servicios (bases de datos, microservicios, frontend)
docker-compose up -d

# Ver registros
docker-compose logs -f

# Detener todos los servicios
docker-compose down
```

### Compilar desde el Código Fuente

#### Módulos Backend

```bash
# Compilar todos los microservicios
cd api-gateway && ./mvnw clean package
cd ../MS-login && ./mvnw clean package
cd ../MS-data && ./mvnw clean package
cd ../MS-kpi && ./mvnw clean package
cd ../MS-reportes && ./mvnw clean package
```

#### Frontend

```bash
cd frontend
npm install
npm run build
npm run dev  # para desarrollo
```

## Configuración de Base de Datos

Inicializar bases de datos usando scripts SQL en el directorio `sql/`:

```bash
# Inicialización de base de datos principal
mysql -u root -p < sql/init.sql

# Bases de datos individuales de microservicios
mysql -u root -p < sql/ms_login.sql
mysql -u root -p < sql/ms_datos.sql
mysql -u root -p < sql/ms_kpi.sql
mysql -u root -p < sql/ms_reportes.sql
```

## Estructura del Proyecto

```
Cordillera3.8/
├── api-gateway/          # API Gateway (Spring Cloud Gateway)
├── MS-login/             # Microservicio de autenticación
├── MS-data/              # Microservicio de gestión de datos
├── MS-kpi/               # Microservicio de seguimiento de KPI
├── MS-reportes/          # Microservicio de reportes
├── frontend/             # Frontend React + Vite
├── sql/                  # Scripts de inicialización de base de datos
├── docker-compose.yml    # Archivo Docker Compose principal
└── build.ps1             # Script de compilación PowerShell
```

## Configuración

### Variables de Entorno

Crear un archivo `.env` en el directorio raíz:

```env
JAVA_VERSION=17
MYSQL_ROOT_PASSWORD=tucontraseña
MYSQL_USER=cordillera
MYSQL_PASSWORD=tucontraseña
SPRING_PROFILES_ACTIVE=dev
```

### Propiedades de Aplicación

Cada microservicio tiene configuración en `src/main/resources/`:
- `application.properties` - Propiedades filtradas por Maven
- `application.yaml` - Configuración YAML

## Endpoints de API

### Autenticación (MS-Login)
- `POST /api/auth/login` - Inicio de sesión de usuario
- `POST /api/auth/register` - Registro de usuario
- `POST /api/auth/refresh` - Renovar token JWT

### Servicio de Datos (MS-Data)
- `GET /api/data/*` - Recuperar datos
- `POST /api/data/*` - Crear datos
- `PUT /api/data/*` - Actualizar datos
- `DELETE /api/data/*` - Eliminar datos

### Servicio de KPI (MS-KPI)
- `GET /api/kpi/*` - Métricas y análisis de KPI

### Servicio de Reportes (MS-Reportes)
- `GET /api/reports/*` - Generar y recuperar reportes

## Desarrollo

### Compilar Servicios Individuales

```bash
cd <service-folder>
./mvnw clean install
./mvnw spring-boot:run
```

### Desarrollo del Frontend

```bash
cd frontend
npm install
npm run dev  # inicia servidor de desarrollo Vite en localhost:5173
```

### Ejecutar Pruebas

```bash
# Pruebas backend
cd <service-folder>
./mvnw test

# Pruebas frontend
cd frontend
npm test
```

## Imágenes Docker

Compilar imágenes personalizadas:

```bash
# API Gateway
docker build -t cordillera-api-gateway ./api-gateway

# Microservicios
docker build -t cordillera-ms-login ./MS-login
docker build -t cordillera-ms-data ./MS-data
docker build -t cordillera-ms-kpi ./MS-kpi
docker build -t cordillera-ms-reportes ./MS-reportes

# Frontend
docker build -t cordillera-frontend ./frontend
```

## Solución de Problemas

### Conflictos de Puertos
Si los puertos ya están en uso, modifica `docker-compose.yml` para usar puertos diferentes.

### Errores de Conexión a Base de Datos
- Asegúrate de que MySQL esté en ejecución
- Verifica las credenciales en `application.properties`
- Verifica la conectividad de red entre contenedores

### Frontend No Se Carga
- Borra la caché del navegador
- Verifica los registros de Nginx: `docker-compose logs nginx`
- Verifica que la puerta de enlace API sea accesible

## Contribución

1. Crear una rama de característica
2. Realizar tus cambios
3. Probar exhaustivamente
4. Enviar una solicitud de extracción

## Licencia

Propietaria - Cordillera3.8

## Soporte

Para preguntas o problemas, contacta al equipo de desarrollo.


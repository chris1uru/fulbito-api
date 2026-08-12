# Fulbito API

Backend inicial de Fulbito construido con Java 21, Spring Boot, Maven, Spring Data JPA,
PostgreSQL, Spring Security y JWT.

## Requisitos

- JDK 21. En esta PC quedo un Temurin 21 portatil en `E:\temurin-21\jdk-21.0.12+8`.
- VS Code con `Extension Pack for Java` y `Spring Boot Extension Pack`.
- La base PostgreSQL creada con el esquema acordado.

Maven no necesita instalarse globalmente: el proyecto incluye Maven Wrapper (`mvnw.cmd`).

## Configuracion local segura (PowerShell)

La contrasena no se guarda en el repositorio. Defini las variables en la terminal antes de ejecutar:

```powershell
$env:DB_URL='jdbc:postgresql://TU_HOST/fulbito_dev?sslmode=require&channelBinding=require'
$env:DB_USERNAME='neondb_owner'
$env:DB_PASSWORD='TU_PASSWORD_ROTADA'
$env:JWT_SECRET='UN_SECRETO_ALEATORIO_LARGO_DE_32_CARACTERES_O_MAS'
```

Luego:

```powershell
cd E:\fulbito_api
.\run-local.ps1
```

Swagger queda disponible en `http://localhost:8080/swagger-ui.html`.

En VS Code, usa `File > Open Folder` y abre `E:\fulbito_api`. La configuracion local
`.vscode/settings.json` ya apunta al JDK portatil. Si copias el proyecto a otra PC,
instala JDK 21 y cambia esa ruta, o configura `JAVA_HOME`.

## Organizacion

- `domain`: entidades JPA que representan las tablas.
- `repository`: acceso a PostgreSQL mediante Spring Data JPA.
- `dto`: contratos JSON de entrada y salida; la API no expone entidades.
- `service`: reglas de negocio, transacciones y autorizacion por propiedad.
- `controller`: rutas HTTP.
- `security`: JWT, BCrypt, CORS y usuario autenticado.
- `error`: respuestas de error consistentes.

## Seguridad

El registro tiene rutas separadas para dueños y jugadores: el cliente nunca puede enviar un rol arbitrario.
Las contrasenas se guardan con BCrypt (factor 12), nunca en texto plano. El JWT contiene el ID y rol,
vence a las ocho horas y debe enviarse como `Authorization: Bearer TOKEN`. La API no usa sesiones ni cookies.

Las comprobaciones de rol no reemplazan las comprobaciones de propiedad: un OWNER solo puede modificar
registros vinculados a sus propios complejos. Los estados sensibles, el precio y el usuario autenticado se
calculan en el servidor.

## Flujo inicial

1. `POST /api/auth/register-owner`
2. Copiar `accessToken` y autorizar Swagger con `Bearer <token>`.
3. `POST /api/owner/venues`
4. `POST /api/owner/venues/{venueId}/courts`
5. `POST /api/owner/venues/{venueId}/opening-hours`
6. `POST /api/reservations`
7. `PATCH /api/reservations/{id}/mark-paid`

Hibernate usa `ddl-auto=validate`: valida el esquema al arrancar pero no crea, borra ni modifica tablas.
Los triggers y constraints de PostgreSQL siguen siendo la defensa final contra solapamientos.

# Fulbito API

Backend inicial de Fulbito construido con Java 21, Spring Boot, Maven, Spring Data JPA,
PostgreSQL, Spring Security y JWT.

## Requisitos

- JDK 21 o superior, disponible mediante `JAVA_HOME` o en el `PATH`.
- VS Code con `Extension Pack for Java` y `Spring Boot Extension Pack`.
- La base PostgreSQL creada con el esquema acordado.

Maven no necesita instalarse globalmente: el proyecto incluye Maven Wrapper (`mvnw.cmd`).
Si no tenes un JDK, podes instalar [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21).
Luego comproba la instalacion con `java -version` y `javac -version`.

## Preparar la base de desarrollo

En una base existente no ejecutes `database/schema.sql`: ese archivo es destructivo y se conserva
solo para reconstrucciones manuales desde cero. Al iniciar la API, Flyway valida y aplica únicamente
las migraciones pendientes de `src/main/resources/db/migration`, sin borrar usuarios ni datos.

En una base completamente vacia, Flyway utiliza `B4__baseline_schema.sql` para crear el esquema
actual y despues aplica las migraciones posteriores.

## Configuracion local segura (PowerShell)

La contrasena no se guarda en el repositorio. Copia `.env.example` como `.env` y completa sus
valores. `.env` esta ignorado por Git y `run-local.ps1` lo carga automaticamente; una variable ya
definida en la terminal tiene prioridad.

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
$secret
```

Copia el resultado en `JWT_SECRET` dentro de `.env`; no lo compartas ni lo subas a Git.

Para las fotos, completa tambien `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y
`CLOUDINARY_API_SECRET` con los valores de tu cuenta. El secreto queda exclusivamente en la API:
la app pide una firma temporal, sube la imagen directamente a Cloudinary y luego la API verifica
el archivo antes de registrarlo. Nunca copies `CLOUDINARY_API_SECRET` al proyecto de Expo.

Luego:

```powershell
.\run-local.ps1
```

Swagger queda disponible en `http://localhost:8080/swagger-ui.html`.

El HTTP local es intencional y no requiere certificados. `REQUIRE_HTTPS` queda en `false` para
desarrollo; al desplegar detras de un proxy o balanceador TLS, configuralo en `true`. La API respeta
los encabezados reenviados por el proxy y entonces rechaza solicitudes que no hayan llegado por HTTPS.

`run-local.ps1` detecta automaticamente el JDK configurado en `JAVA_HOME` o disponible
en el `PATH`; no depende de la ubicacion del proyecto ni de una ruta fija del JDK.

## Datos de demostracion

`database/seed-dev.sql` agrega de forma idempotente un complejo, canchas, horarios, reservas
manuales y un bloqueo para probar disponibilidad. No elimina ni modifica usuarios, y no se ejecuta
automaticamente: usalo solamente en la base de desarrollo desde el SQL Editor de Neon.

## Organizacion

- `domain`: entidades JPA que representan las tablas.
- `repository`: acceso a PostgreSQL mediante Spring Data JPA.
- `dto`: contratos JSON de entrada y salida; la API no expone entidades.
- `service`: reglas de negocio, transacciones y autorizacion por propiedad.
- `controller`: rutas HTTP.
- `security`: JWT, BCrypt, CORS y usuario autenticado.
- `error`: respuestas de error consistentes.

## Seguridad

Solo los jugadores pueden registrarse públicamente. Los dueños son creados por un administrador,
con email y cédula únicos, y luego se asignan explícitamente a uno o más complejos.
Las contrasenas se guardan con BCrypt (factor 12), nunca en texto plano. El JWT contiene el ID y rol,
vence segun `JWT_EXPIRATION` (dos horas por defecto) y debe enviarse como
`Authorization: Bearer TOKEN`. La API no usa sesiones ni cookies.

Las comprobaciones de rol no reemplazan las comprobaciones de propiedad: un OWNER solo puede modificar
registros vinculados a sus propios complejos. Los estados sensibles, el precio y el usuario autenticado se
calculan en el servidor.

Cada complejo admite hasta 8 imagenes y cada cancha hasta 5. Solo OWNER y ADMIN pueden pedir
firmas, registrar o eliminar archivos, y un OWNER debe ser responsable del complejo. La API acepta
JPG, PNG o WebP de hasta 5 MB y 2000 px por lado; la app los convierte previamente a JPEG de hasta
1600 px para reducir datos y consumo del plan gratuito. Al eliminar una imagen tambien se elimina
su recurso de Cloudinary.

## Flujo inicial

1. Iniciar sesión como `ADMIN` y copiar el `accessToken`.
2. `POST /api/admin/users` para crear un usuario `OWNER`.
3. `POST /api/admin/venues` indicando el `ownerId`.
4. El dueño administra sus complejos mediante `/api/owner/**`.
5. `POST /api/owner/venues/{venueId}/courts`.
6. `POST /api/owner/venues/{venueId}/opening-hours`.
7. `POST /api/reservations`.
8. `PATCH /api/reservations/{id}/mark-paid`.

Hibernate usa `ddl-auto=validate`: valida el esquema al arrancar pero no crea, borra ni modifica tablas.
Flyway aplica migraciones incrementales antes de esa validación y toma el esquema inicial como versión 1.
Los triggers y constraints de PostgreSQL siguen siendo la defensa final contra solapamientos.

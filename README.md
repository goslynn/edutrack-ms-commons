# edutrack-ms-commons

Librería Maven compartida que centraliza el código transversal a todos los microservicios de EduTrack. Evita duplicar patrones de seguridad, manejo de errores, contexto de request, persistencia y comunicación inter-servicio.

**Artefacto:** `cl.duocuc.edutrack:edutrack-ms-commons:1.0.0`

> Esta librería **no contiene reglas de negocio** ni referencias a ningún dominio específico. Es código de infraestructura puro, consumible por cualquier microservicio del monorepo.

---

## Stack

| Capa | Tecnología |
|---|---|
| Runtime | Java 21 + Quarkus 3.34.6 |
| DI | Quarkus ArC (CDI) |
| REST client | RESTEasy Reactive Client + Jackson |
| ORM | Hibernate ORM Panache |
| Validación | Hibernate Validator (Jakarta Bean Validation) |
| Build | Maven Wrapper (`./mvnw`) |

---

## Instalar en el repositorio local

Todos los microservicios dependen de este artefacto. Antes de compilar cualquier MS, instálalo:

```bash
# Desde este directorio
./mvnw install -DskipTests
```

---

## Qué incluye

### `infrastructure.security` — Autorización Unix-style

Sistema de permisos con flags numéricos (`READ=4, WRITE=2, EXECUTE=1`):

- **`@RequirePermission(resource = "...", value = Permission.READ)`** — anotación para endpoints JAX-RS. El filtro la intercepta y consulta a Auth Service.
- **`Permission`** — enum con los tres flags y lógica de bits.
- **`PermissionEvaluator`** — contrato CDI que cada MS puede implementar. La implementación por defecto (`RemotePermissionEvaluator`) consulta `GET /auth/access`.
- **`ResourceIds.ALL`** — comodín `"*"` para grants que cubren todos los recursos.

### `infrastructure.context` — Cabeceras internas del Gateway

El Gateway propaga la identidad autenticada como headers internos. Esta capa los interpreta una sola vez por request:

- **`InternalHeader`** — enum con los nombres de cabecera (`X-User-Id`, `X-User-Roles`, `X-Correlation-Id`). Única fuente de verdad.
- **`RequestContext`** — bean `@RequestScoped` inyectable. Lee y valida los headers en `@PostConstruct`; los expone tipados vía `headers()`.
- **`HeaderValidationMode`** — `EAGER` (default): header malformado aborta con `400`. `WARN`: loguea y trata como ausente.

> **Prohibido** leer headers `X-...` a mano con `@HeaderParam` o `getHeaderString()`. Siempre inyectar `RequestContext`.

### `infrastructure.exception` — Manejo de errores global

Envelope JSON único para todos los errores de la plataforma:

- **`GlobalExceptionMappers`** — `@ServerExceptionMapper` por tipo: `DomainException`, `ConstraintViolationException`, `WebApplicationException`, `Throwable`.
- **`ErrorResponse`** — envelope con `timestamp`, `status`, `error`, `code`, `message`, `path`, `metadata`, `trace` (solo si `edutrack.errors.expose-stacktrace=true`).
- **`DomainException`** — excepción base para reglas de negocio. Sugar: `ConflictException` (409), `NotFoundException` (404), `ForbiddenException` (403).
- Convención del `code`: `<MS>.<ENTIDAD>.<CONDICION>` en SCREAMING_SNAKE (ej: `AUTH.USER.EMAIL_EXISTS`). Estable entre versiones.

### `infrastructure.jackson` — Vistas de serialización

Sistema de vistas `@JsonView` para controlar qué campos viajan en cada endpoint sin crear DTOs adicionales:

- **`Views`** — jerarquía estándar: `Base`, `Extra`, `Detailed extends Base`, `Create extends Base`, `List extends Base+Extra`, `Patch extends Base+Extra`, `Update extends Base`, `Admin extends Base+Extra`, `Internal`.
- **`JacksonCustomConfig`** — fija `Views.Base` como vista por defecto (`DEFAULT_VIEW_INCLUSION=false`). Los endpoints que necesiten otra vista la anotan con `@JsonView(Views.XXX.class)`.

### `infrastructure.validation` — Grupos de validación

- **`Validations`** — interfaz con grupo `OnCreate` y secuencia `Create`. Permite que un mismo DTO tenga campos requeridos en creación pero opcionales en actualización usando `@ConvertGroup`.

### `infrastructure.persistence` — Superclases de entidades

Jerarquía `@MappedSuperclass` para herencia DRY:

- **`CreatableEntity`** — `id` (UUID, PK) + `createdAt` + callback `@PrePersist`.
- **`AuditableEntity extends CreatableEntity`** — agrega `updatedAt`, `creatorUser`, `updaterUser` + callback `@PreUpdate`.

Cada entidad hereda solo lo que aplica a su semántica (las entidades inmutables como tokens heredan solo `CreatableEntity`).

### `infrastructure.discovery` — Descubrimiento de servicios

Configuración automática de URLs inter-servicio:

- **`ServiceIds`** — catálogo de nombres de servicio (`"auth"`, `"course"`, `"student"`, …).
- **`DiscoveryConfigSourceFactory`** — sustituye `{service}` en el patrón `edutrack.discovery.pattern`. Dev: `{service}:8080`; Fly.io: `edutrack-{service}.fly.internal:8080`.
- **`IdentityHeadersFactory`** — reenvía `X-User-Id` y `X-User-Roles` en llamadas inter-MS.
- **`RemotePermissionEvaluator`** — implementación de `PermissionEvaluator` que consulta `GET /auth/access`.
- **`ErrorPropagationClientFilter`** — reconstruye `DomainException` desde el `ErrorResponse` de un MS upstream.

### `clients` — SDKs de clientes inter-servicio

Fuera de `infrastructure` porque sí conocen la forma de cada API:

- **`AuthClient`** — REST client declarativo hacia Auth Service.
- **`AttendanceClient`** — REST client hacia Attendance Service.

Se inyectan con `@Inject @RestClient AuthClient`.

---

## Agregar commons como dependencia

En el `pom.xml` de cada microservicio:

```xml
<dependency>
    <groupId>cl.duocuc.edutrack</groupId>
    <artifactId>edutrack-ms-commons</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Reglas de extensión

Cuando un MS necesita un comportamiento específico (ej: evaluación de permisos propia), se implementa el contrato CDI definido en `infrastructure`:

```java
@ApplicationScoped
public class MiPermissionEvaluator implements PermissionEvaluator {
    // implementación específica del MS
}
```

**Nunca** se modifica el código de `infrastructure` para agregar lógica de dominio específica.

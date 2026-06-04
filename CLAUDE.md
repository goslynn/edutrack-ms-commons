# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Propósito

Este repo es la **librería compartida** de EduTrack: el artefacto `cl.duocuc.edutrack:edutrack-ms-commons` que extrae el paquete `cl.duocuc.edutrack.ms.infrastructure` y lo distribuye como dependencia a cada microservicio del monorepo (`auth/`, y los próximos: Course, Student, Content, Assessment, Attendance, Annotation, Notification, Report).

Mientras el artefacto no esté publicado, cada MS duplica este código en su propio `infrastructure/`. **Los cambios hechos aquí deben preservar nombres y forma** para que la migración a dependencia sea un swap directo. Ver `../CLAUDE.md` y `../doc/` para el contexto del monorepo.

## Contrato del paquete `infrastructure`

Reglas inviolables (rompen el contrato con todos los MS consumidores):

- **No importar `cl.duocuc.edutrack.ms.<servicio>.*`.** Ningún archivo bajo `infrastructure` puede depender de un dominio concreto. Puntos de extensión específicos del MS host se exponen como **contratos CDI** (p. ej. `security.PermissionEvaluator`) que cada MS implementa.
- **No referenciar nombres específicos de un MS.** Identificadores propios de un MS (UUIDs de recursos, vistas/grupos de validación de su dominio — p. ej. `AuthResourceId`, `AuthViews`, `AuthValidations`) viven en el MS. Aquí solo lo común: wildcard `ResourceIds.ALL`/`ALL_UUID`, jerarquía `Views.Base/Extra/Detailed/...`, grupo `Validations.OnCreate` con su secuencia `Validations.Create`.
- **Subpaquetes por responsabilidad técnica** (no por dominio): `security`, `context`, `exception`, `jackson`, `validation`, `persistence`, `discovery`.
- **`clients.*` es la excepción deliberada al no-dominio.** El paquete `cl.duocuc.edutrack.ms.clients.<servicio>` (fuera de `infrastructure`, mismo artefacto) **sí** conoce la forma de la API de cada MS. No vive bajo `infrastructure` justamente para no romper su contrato base. Ahí van los SDKs de cliente (`CourseClient`, etc.).

## Mapa de subpaquetes

- `security/` — Autorización Unix-style. `@RequirePermission(resource=<resource-key>, value=Permission.READ|WRITE|EXECUTE)` (la `resource-key` es una clave estable de texto como `auth.users`, no un UUID), enum `Permission` (bits `r=4,w=2,x=1`), `ResourceIds` (wildcard `ALL` = `"*"`), contrato `PermissionEvaluator` y el `RequirePermissionFilter` que lo consume. La **misma** lógica de bits (flags efectivos OR comodín) debe alimentar también el endpoint `GET /auth/access` del Auth Service — no se duplica.
- `context/` — Intérprete único de cabeceras internas del API Gateway. Enum `InternalHeader` (única fuente de verdad de nombres `X-...`), record inmutable `RequestHeaders`, bean `@RequestScoped` proxyable `RequestContext` que valida una vez por request en `@PostConstruct`. Modo de validación configurable por `edutrack.headers.validation.mode` (`EAGER` default → `400`, `WARN` → loguea y trata como ausente). Cabecera ausente ⇒ valor vacío, nunca `null`, nunca fallo. **Nota CDI/ArC:** el bean inyectable es el holder `RequestContext`, no un `@Produces` del record (los records son `final` y no proxiables).
- `exception/` — `GlobalExceptionMappers` con `@ServerExceptionMapper` por tipo (`DomainException`, `ConstraintViolationException`, `WebApplicationException`, `Throwable`) → envelope único `ErrorResponse` (`timestamp`, `status`, `error`, `code`, `message`, `path`, `metadata`, `trace`). `trace` solo cuando `edutrack.errors.expose-stacktrace=true` (default `false`). Jerarquía `DomainException` + sugar `ConflictException` (409), `NotFoundException` (404), `ForbiddenException` (403). Convención de `code`: `<MS>.<ENTIDAD>.<CONDICION>` SCREAMING_SNAKE, estable.
- `jackson/` — Interfaz `Views` (`Base`, `Extra`, `Detailed extends Base`, `Create extends Base`, `List extends Base,Extra`, `Patch extends Base,Extra`, `Update extends Base`, `Admin extends Base,Extra`, `Internal`) + `JacksonCustomConfig` que fija `Views.Base` como vista por defecto y desactiva `MapperFeature.DEFAULT_VIEW_INCLUSION`. Cada MS extiende con vistas de dominio (`AuthViews.Login extends Views.Base`).
- `validation/` — Interfaz `Validations` con `OnCreate` y la secuencia `Create`. Grupos específicos del dominio viven en el MS.
- `persistence/` — Superclases `@MappedSuperclass` para herencia DRY de entidades: `CreatableEntity` (`id` UUID + `createdAt`, para entidades inmutables como tokens) → `AuditableEntity extends CreatableEntity` agrega `updatedAt`. Callbacks JPA (`@PrePersist`, `@PreUpdate`) viajan con la superclase. El DDL no cambia.
- `discovery/` — Comunicación HTTP inter-servicio sobre **Fly.io DNS** (sin registry: ni Consul ni Stork). `ServiceIds` = catálogo de nombres lógicos **bare** (`"auth"`, `"course"`…, = primer segmento del path del gateway) + `ServiceIds.ALL` (set iterable). Los clients son **declarativos** (`@RegisterRestClient(configKey = ServiceIds.X)`, inyectados con `@Inject @RestClient`): su `quarkus.rest-client.<x>.url` **no** se escribe a mano, la deriva `DiscoveryConfigSourceFactory` (un `ConfigSourceFactory` de SmallRye registrado por `ServiceLoader` en `META-INF/services/`, que viaja en el JAR — los MS no lo declaran) sustituyendo `{service}` en `edutrack.discovery.pattern` (default `edutrack-{service}.fly.internal:8080`, override profile-aware `%dev`→`{service}:8080`, `scheme` http/https). Al ser declarativos heredan build-time, native, Fault Tolerance (`@CircuitBreaker`/`@Retry`/`@Timeout`) y OTel. `ServiceRegistry` (`@ApplicationScoped`) queda solo como helper de propagación de error: `readOrThrow(Response, type)` reconstruye `DomainException` desde el `ErrorResponse` del upstream. Identidad inter-servicio: `@RegisterClientHeaders(IdentityHeadersFactory.class)` reenvía `X-User-Id`/`X-User-Roles` desde el `RequestContext`. Las llamadas MS↔MS son directas app-a-app, **no** pasan por el gateway. `AuthClient` (en `clients.*`) es el SDK completo del Auth Service: cubre toda su API (auth, access, users, roles, permisos, JWKS), retorna `Response` crudo y recibe cuerpos como `Object` para no acoplarse a los DTOs de Auth; reenvía identidad con `@RegisterClientHeaders(IdentityHeadersFactory.class)`. Su `GET /auth/access` lo consume `RemotePermissionEvaluator` para la authz transversal. Estilo de client: interfaz tipada que retorna `Response` + DTO tolerant-reader del consumidor.

## Comandos

```bash
./mvnw quarkus:dev                       # Dev mode con hot reload
./mvnw package                           # Build (target/quarkus-app/)
./mvnw package -Dquarkus.package.jar.type=uber-jar
./mvnw test                              # Tests unitarios
./mvnw verify                            # Tests de integración (-DskipITs=false implícito en verify)
./mvnw test -Dtest=NombreDelTest         # Un solo test
./mvnw package -Dnative                  # Native executable
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Stack

Quarkus 3.35.3 · Java 21 · `quarkus-rest-jackson`, `quarkus-arc`, `quarkus-hibernate-validator`, `quarkus-hibernate-orm-panache`. Test: `quarkus-junit`, `rest-assured`.

## Al editar este repo

- Si añades una clase aquí, **verifica que no exista una variante divergente** en `../auth/src/main/java/cl/duocuc/edutrack/ms/infrastructure/` (duplicación intencional). Si cambia el contrato, sincroniza ambas o avisa al usuario.
- Cualquier nuevo punto que pudiera necesitar un MS específico se modela como **interfaz CDI** + implementación en el MS, nunca como referencia directa.
- Lee el `package-info.java` de `infrastructure` antes de mover o renombrar — codifica el contrato.

/**
 * Estándar de <b>comunicación HTTP inter-servicio</b> de EduTrack sobre la
 * infraestructura de Fly.io. Reemplaza al esquema previo (SmallRye Stork +
 * Consul, pensado para Kubernetes/AWS): la nueva infra no tiene registry — el
 * descubrimiento es <b>DNS puro</b>.
 *
 * <h2>Descubrimiento: DNS privado de Fly.io (sin registry)</h2>
 * <p>Cada microservicio es una app independiente en Fly.io, resolvible por DNS
 * dentro de la red privada 6PN/WireGuard de la organización como
 * {@code <app>.fly.internal}. El nombre de app sigue el contrato del gateway:
 * {@code edutrack-<service>}, donde {@code <service>} es el nombre lógico bare
 * de {@link cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds}.
 * "Descubrir" un servicio es simplemente construir su URL — no hay nada que
 * consultar ni registrar. El balanceo entre instancias lo hace Fly por DNS
 * (todas las instancias vivas responden a {@code <app>.fly.internal}; usar
 * {@code top3.nearest.of.<app>.fly.internal} si se quiere afinidad regional).</p>
 *
 * <h2>Contrato</h2>
 * <ol>
 *   <li>Todo host destino se resuelve vía el patrón
 *       {@code edutrack.discovery.pattern}, sustituyendo en su placeholder
 *       {@code {service}} un id de
 *       {@link cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds}.
 *       <b>Prohibido</b> hardcodear hosts, IPs o nombres DNS de otros MS en el
 *       código o en {@code application.properties}.</li>
 *   <li>Los REST Client son <b>declarativos</b>:
 *       {@code @RegisterRestClient(configKey = ServiceIds.X)} y se inyectan con
 *       {@code @Inject @RestClient}. <b>No</b> se construyen programáticamente y
 *       <b>no</b> se escribe su {@code quarkus.rest-client.<x>.url} a mano: la
 *       deriva del patrón el
 *       {@link cl.duocuc.edutrack.ms.infrastructure.discovery.DiscoveryConfigSourceFactory}
 *       (un {@code ConfigSourceFactory} de SmallRye registrado por
 *       {@code ServiceLoader} en el JAR de la librería). Al ser declarativos
 *       heredan build-time, native-image, SmallRye Fault Tolerance
 *       ({@code @CircuitBreaker}/{@code @Retry}/{@code @Timeout} en la interfaz)
 *       y OpenTelemetry sin código extra.</li>
 *   <li>Las llamadas MS↔MS son <b>directas app-a-app</b> por
 *       {@code *.fly.internal}: <b>no</b> pasan por el API Gateway. La identidad
 *       ya autenticada se reenvía como cabeceras internas
 *       ({@code X-User-Id} / {@code X-User-Roles}) vía
 *       {@code @RegisterClientHeaders(}{@link cl.duocuc.edutrack.ms.infrastructure.discovery.IdentityHeadersFactory}{@code .class)},
 *       que las toma del
 *       {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestContext}.</li>
 * </ol>
 *
 * <h2>Configuración del consumidor</h2>
 * <p>El JAR de la librería trae el default productivo en
 * {@code META-INF/microprofile-config.properties}:</p>
 * <pre>{@code
 * edutrack.discovery.scheme=http
 * edutrack.discovery.pattern=edutrack-{service}.fly.internal:8080
 * }</pre>
 * <p>Son los <b>dos únicos knobs</b> de discovery —{@code scheme} (http/https) y
 * {@code pattern} (template de host)— y bastan para mover toda la comunicación
 * entre entornos. Un MS consumidor no necesita declarar nada en producción. En
 * local (docker-compose, donde el contenedor se llama {@code auth}, no
 * {@code edutrack-auth}) sobreescribe el patrón en su propio
 * {@code application.properties}; la resolución del factory es profile-aware:</p>
 * <pre>{@code
 * %dev.edutrack.discovery.pattern={service}:8080
 * %prod.edutrack.discovery.scheme=https
 * }</pre>
 * <p>El archivo {@code META-INF/services/io.smallrye.config.ConfigSourceFactory}
 * que activa el factory viaja en <b>este</b> JAR: cualquier MS que dependa de la
 * librería lo obtiene por classpath automáticamente — <b>no</b> lo declara cada
 * MS. (Mientras el código se duplique en cada MS en vez de consumirse como
 * dependencia, esa duplicación debe incluir también ese archivo de
 * {@code META-INF/services}.)</p>
 *
 * <h2>Forma de los clients (interfaz tipada + {@code Response})</h2>
 * <p>Las interfaces de client por MS viven en {@code cl.duocuc.edutrack.ms.clients.<servicio>}
 * (fuera de {@code infrastructure}, porque conocen forma de dominio). Llevan
 * {@code @RegisterRestClient(configKey = ServiceIds.X)}, declaran
 * path/verbo/headers tipados pero retornan {@link jakarta.ws.rs.core.Response}:
 * el consumidor extrae el cuerpo con su <b>propio</b> DTO tolerant-reader vía
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.HTTPClientUtils#readOrThrow(jakarta.ws.rs.core.Response, Class)},
 * que en no-2xx reconstruye el {@code DomainException} desde el envelope
 * {@code ErrorResponse} del upstream. Ver el {@code package-info} de
 * {@code clients} para el contrato detallado.</p>
 *
 * <h2>Ejemplo dentro de esta misma librería</h2>
 * <p>{@link cl.duocuc.edutrack.ms.clients.AuthAccessClient} es
 * el caso límite legítimo que vive en {@code infrastructure} (authz transversal,
 * no dominio): client declarativo con
 * {@code @RegisterRestClient(configKey = ServiceIds.AUTH)} que inyecta y consume
 * {@link cl.duocuc.edutrack.ms.infrastructure.context.RemoteSuperUserResolver}
 * vía {@code @Inject @RestClient}.</p>
 *
 * <h2>Resiliencia</h2>
 * <p>El descubrimiento es ortogonal a la resiliencia: el patrón resuelve
 * <i>dónde</i> está el servicio; el caller decide <i>cómo</i> tolera fallos. Al
 * ser declarativos, los clients aceptan las anotaciones de SmallRye Fault
 * Tolerance ({@code @Timeout}, {@code @Retry}, {@code @CircuitBreaker}) sobre los
 * métodos de la interfaz — la integración que el builder programático <b>no</b>
 * permitía. No se impone una política global desde acá.</p>
 */
package cl.duocuc.edutrack.ms.infrastructure.discovery;

/**
 * <b>SDKs de cliente HTTP por microservicio</b> de EduTrack. A diferencia de
 * {@code cl.duocuc.edutrack.ms.infrastructure} —que tiene prohibido conocer
 * dominio— este paquete <b>sí</b> conoce la forma de la API de cada MS: aquí
 * vive {@code clients.course.CourseClient}, {@code clients.student.StudentClient},
 * etc. Se distribuye en el mismo artefacto {@code edutrack-ms-commons} pero
 * fuera de {@code infrastructure} justamente para no contaminar el contrato
 * base no-dominio.
 *
 * <h2>Cómo un MS-x le habla a un MS-y</h2>
 * <p>El descubrimiento (la URL del MS-y) lo deriva del patrón
 * {@code edutrack.discovery.pattern} el
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.DiscoveryConfigSourceFactory},
 * que publica {@code quarkus.rest-client.<servicio>.url} por cada
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds} (ver su
 * {@code package-info}). El client es <b>declarativo</b>: aquí solo se declara la
 * interfaz tipada con {@code @RegisterRestClient(configKey = ServiceIds.X)} — sin
 * productor CDI ni construcción manual; se inyecta con {@code @Inject @RestClient}.</p>
 *
 * <h2>Estilo: interfaz tipada que retorna {@code Response}</h2>
 * <p>Cada client declara path/verbo/headers <b>tipados</b> (chequeados por el
 * compilador, descubribles desde el tipo) pero retorna
 * {@link jakarta.ws.rs.core.Response}. La <b>forma</b> de la respuesta no la
 * dicta el client: cada consumidor la extrae con su <b>propio</b> DTO
 * tolerant-reader vía
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceRegistry#readOrThrow(jakarta.ws.rs.core.Response, Class)}.
 * Así el productor del MS-y puede evolucionar su payload sin romper consumidores,
 * y dos consumidores distintos pueden bindear slices distintos sin compartir un
 * DTO canónico.</p>
 *
 * <h3>1. La interfaz declarativa (en {@code clients.<servicio>})</h3>
 * <p>{@code configKey} debe ser el id de {@code ServiceIds} (así calza con la
 * propiedad {@code quarkus.rest-client.course.url} que genera el factory). La
 * identidad se propaga sola con {@code @RegisterClientHeaders}: no se declaran
 * {@code @HeaderParam("X-...")} a mano.</p>
 * <pre>{@code
 * @RegisterRestClient(configKey = ServiceIds.COURSE)
 * @RegisterClientHeaders(IdentityHeadersFactory.class)
 * @Path("/courses")
 * public interface CourseClient {
 *     @GET @Path("/{id}")
 *     @Produces(MediaType.APPLICATION_JSON)
 *     @Timeout(2000)
 *     @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5)
 *     Response getById(@PathParam("id") UUID id);
 * }
 * }</pre>
 *
 * <h3>2. La inyección (directa, sin productor)</h3>
 * <pre>{@code
 * @Inject @RestClient CourseClient courseClient;
 * }</pre>
 *
 * <h3>3. El DTO tolerant-reader (lo declara el consumidor, mínimo)</h3>
 * <pre>{@code
 * @JsonIgnoreProperties(ignoreUnknown = true)
 * public record CourseBrief(
 *     @JsonView(Views.Base.class) UUID id,
 *     @JsonView(Views.Base.class) String name
 * ) {}
 * }</pre>
 * <p>Cada campo lleva {@code @JsonView(Views.Base.class)} porque
 * {@code JacksonCustomConfig} desactiva {@code DEFAULT_VIEW_INCLUSION}: sin la
 * vista, Jackson no deserializaría el campo.</p>
 *
 * <h3>4. El call site (la identidad la pone el headers factory; extrae con readOrThrow)</h3>
 * <pre>{@code
 * Response r = courseClient.getById(courseId);
 * CourseBrief course = registry.readOrThrow(r, CourseBrief.class);
 * }</pre>
 * <p>{@code readOrThrow} extrae el cuerpo en 2xx; en no-2xx lanza
 * {@link cl.duocuc.edutrack.ms.infrastructure.exception.DomainException}
 * reconstruido desde el envelope {@code ErrorResponse} del MS-y, preservando su
 * {@code code} de dominio. Cuando un endpoint necesita headers o el status de
 * la respuesta, se usa la {@code Response} cruda en vez de {@code readOrThrow}.</p>
 */
package cl.duocuc.edutrack.ms.clients;

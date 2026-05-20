package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Handler global de excepciones del microservicio. Implementa los mappers de
 * Quarkus REST ({@link ServerExceptionMapper}) que transforman cualquier
 * excepción que escape de un recurso JAX-RS en un {@link ErrorResponse} con
 * forma estable.
 *
 * <h3>Resolución por tipo</h3>
 * <p>Quarkus REST resuelve cada excepción al mapper más específico que tipa
 * un supertipo de la excepción lanzada. Como consecuencia, la lista efectiva
 * de orden de aplicación es:</p>
 * <ul>
 *   <li>{@link #mapDomain DomainException} ⇒ usa
 *       {@link DomainException#status() status},
 *       {@link DomainException#code() code},
 *       {@link DomainException#getMessage() message} y
 *       {@link DomainException#metadata() metadata}. Loguea a nivel
 *       {@code DEBUG}.</li>
 *   <li>{@link #mapConstraintViolation ConstraintViolationException} (Bean
 *       Validation) ⇒ status {@code 400}, code
 *       {@code "VALIDATION.CONSTRAINT"}, message
 *       {@code "Request validation failed"} y metadata con la lista
 *       {@code violations[]} (cada entrada es {@code {"path": ..., "message": ...}}).
 *       Loguea a nivel {@code DEBUG}.</li>
 *   <li>{@link #mapWebApplication WebApplicationException} ⇒ status de la
 *       {@link Response} que carga (o {@code 500} si no hay response). El
 *       envelope se emite <b>sin</b> {@code code} y con mensaje genérico
 *       (la <i>reason phrase</i> para 4xx, {@code "Internal server error"}
 *       para 5xx): el {@code message} de un WAE genérico es ruidoso
 *       ({@code "HTTP 404 Not Found"}). Para devolver un mensaje útil, lanzar
 *       una subclase de {@link DomainException}. Loguea a nivel {@code DEBUG}.</li>
 *   <li>{@link #mapUnhandled Throwable} ⇒ red de seguridad. Status {@code 500},
 *       code {@code "INTERNAL.UNEXPECTED"}, message
 *       {@code "Internal server error"}. Loguea a nivel {@code ERROR} con
 *       traza completa.</li>
 * </ul>
 *
 * <h3>Exposición del stack trace</h3>
 * <p>El campo {@link ErrorResponse#trace()} solo se incluye cuando
 * {@code edutrack.errors.expose-stacktrace=true}. El default es {@code false}
 * para no filtrar paquetes/clases internas ni inflar el body en producción.
 * Cuando está activo, se incluyen hasta {@value #MAX_TRACE_FRAMES} frames
 * (cada uno via {@link StackTraceElement#toString()}).</p>
 *
 * <h3>Registro como bean</h3>
 * <p>La clase es {@code @ApplicationScoped} con métodos
 * {@code @ServerExceptionMapper}: por convención de Quarkus REST, basta con
 * tenerla en el classpath para que sus mappers queden registrados. No es
 * necesario activarla manualmente.</p>
 */
@ApplicationScoped
public class GlobalExceptionMappers {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMappers.class);

    /** Máximo de frames de stack trace expuestos (cuando el flag está activo). */
    private static final int MAX_TRACE_FRAMES = 25;

    @ConfigProperty(name = "edutrack.errors.expose-stacktrace", defaultValue = "false")
    boolean exposeStacktrace;

    /**
     * Mapper para {@link DomainException}. Respeta el status, code, message y
     * metadata que la excepción acarrea.
     */
    @ServerExceptionMapper
    public Response mapDomain(DomainException ex, UriInfo uri) {
        LOG.debugf(ex, "DomainException %s -> %d", ex.code(), ex.status());
        return build(ex.status(), ex.code(), ex.getMessage(), uri, ex.metadata(), ex);
    }

    /**
     * Mapper para {@link ConstraintViolationException} (Bean Validation):
     * convierte cada violación a {@code {"path", "message"}} y las acumula en
     * la metadata como {@code violations[]}.
     */
    @ServerExceptionMapper
    public Response mapConstraintViolation(ConstraintViolationException ex, UriInfo uri) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
            .map(this::renderViolation)
            .toList();
        Map<String, Object> meta = Map.of("violations", violations);
        LOG.debugf("ConstraintViolation %s", violations);
        return build(400, "VALIDATION.CONSTRAINT",
            "Request validation failed", uri, meta, ex);
    }

    /**
     * Mapper para {@link WebApplicationException} y subclases JAX-RS estándar
     * ({@code NotFoundException}, {@code ForbiddenException} del paquete
     * {@code jakarta.ws.rs}, etc.). Responde con el status de la
     * {@link Response} que la excepción carga; si no hay response, {@code 500}.
     * No expone el {@code message} de la excepción (suele ser ruido como
     * {@code "HTTP 404 Not Found"}): emite la reason phrase del status o
     * {@code "Internal server error"} para 5xx.
     */
    @ServerExceptionMapper
    public Response mapWebApplication(WebApplicationException ex, UriInfo uri) {
        int status = ex.getResponse() != null
            ? ex.getResponse().getStatus()
            : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        // No exponemos message del WAE genérico (suele ser ruido tipo "HTTP 404 Not Found");
        // si el caller quiere un mensaje útil, debería lanzar DomainException.
        String msg = status >= 500 ? "Internal server error" : reasonPhrase(status);
        LOG.debugf(ex, "WebApplicationException -> %d", status);
        return build(status, null, msg, uri, Map.of(), ex);
    }

    /**
     * Red de seguridad: cualquier {@link Throwable} no cubierto por los
     * mappers anteriores se convierte en {@code 500} con code
     * {@code "INTERNAL.UNEXPECTED"}. Loguea a nivel {@code ERROR} con la
     * traza completa; el body solo expone el stack trace si
     * {@code edutrack.errors.expose-stacktrace=true}.
     */
    @ServerExceptionMapper
    public Response mapUnhandled(Throwable ex, UriInfo uri) {
        LOG.errorf(ex, "Unhandled exception on %s", path(uri));
        return build(500, "INTERNAL.UNEXPECTED",
            "Internal server error", uri, Map.of(), ex);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Response build(int status, String code, String message, UriInfo uri,
                           Map<String, Object> metadata, Throwable ex) {
        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            status,
            reasonPhrase(status),
            code,
            message,
            path(uri),
            metadata == null || metadata.isEmpty() ? null : new LinkedHashMap<>(metadata),
            exposeStacktrace ? renderTrace(ex) : null
        );
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private Map<String, String> renderViolation(ConstraintViolation<?> v) {
        return Map.of(
            "path", v.getPropertyPath().toString(),
            "message", v.getMessage()
        );
    }

    private List<String> renderTrace(Throwable ex) {
        if (ex == null) return null;
        return Stream.of(ex.getStackTrace())
            .limit(MAX_TRACE_FRAMES)
            .map(StackTraceElement::toString)
            .toList();
    }

    private static String path(UriInfo uri) {
        return uri == null ? null : "/" + uri.getPath();
    }

    private static String reasonPhrase(int status) {
        Response.Status s = Response.Status.fromStatusCode(status);
        return s == null ? "HTTP " + status : s.getReasonPhrase();
    }
}

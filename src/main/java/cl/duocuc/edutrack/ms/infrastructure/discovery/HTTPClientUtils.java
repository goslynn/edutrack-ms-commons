package cl.duocuc.edutrack.ms.infrastructure.discovery;

import cl.duocuc.edutrack.ms.infrastructure.exception.DomainException;
import cl.duocuc.edutrack.ms.infrastructure.exception.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

/**
 * Helper de <b>propagación de errores</b> para clients inter-servicio de EduTrack.
 *
 * <h2>De "registry" a helper de errores</h2>
 * <p>El descubrimiento y la construcción de clients ya <b>no</b> viven aquí: los
 * clients son declarativos ({@code @RegisterRestClient(configKey = ServiceIds.X)})
 * y su URL la deriva {@link DiscoveryConfigSourceFactory} del patrón
 * {@code edutrack.discovery.pattern}. Lo que esta clase conserva —y sigue siendo
 * necesario— es el puente de errores entre servicios.</p>
 *
 * <h2>Por qué hace falta</h2>
 * <p>El estilo de client de la plataforma es <i>interfaz tipada que retorna
 * {@link Response}</i> (paths chequeados, headers disponibles, forma de la
 * respuesta declarada por el consumidor vía {@code readEntity}). Como un método
 * que retorna {@code Response} <b>no dispara</b> los
 * {@code ResponseExceptionMapper} de MP REST Client, el chequeo de status se
 * centraliza en {@link #readOrThrow(Response, Class)}: en no-2xx <b>reconstruye</b>
 * el {@link DomainException} a partir del envelope {@link ErrorResponse} del
 * upstream, preservando su {@code code} de dominio. Así un
 * {@code 409 AUTH.USER.EMAIL_EXISTS} de un MS aterriza en el consumidor como el
 * mismo {@code code}, y el {@code GlobalExceptionMappers} del consumidor lo
 * vuelve a serializar transparente.</p>
 *
 * <pre>{@code
 * @Inject ServiceRegistry registry;
 * ...
 * Response r = courseClient.getById(courseId);
 * CourseBrief course = registry.readOrThrow(r, CourseBrief.class);
 * }</pre>
 *
 * <p>Es {@link ApplicationScoped} y sin estado: cuando un endpoint necesita el
 * status o los headers crudos de la respuesta, no usa {@code readOrThrow} sino la
 * {@link Response} directamente.</p>
 */
@ApplicationScoped
public class HTTPClientUtils {

    /**
     * Lee el cuerpo de una respuesta exitosa al tipo dado, o lanza
     * {@link DomainException} reconstruido desde el envelope del upstream si el
     * status no es 2xx.
     *
     * @param response respuesta de un client inter-servicio
     * @param type     tipo (tolerant reader) al que deserializar el cuerpo 2xx
     * @return entidad deserializada en caso 2xx
     * @throws DomainException en cualquier status no-2xx
     */
    public <T> T readOrThrow(Response response, Class<T> type) {
        if (isSuccessful(response)) {
            return response.readEntity(type);
        }
        throw toDomainException(response);
    }

    /**
     * Variante de {@link #readOrThrow(Response, Class)} para tipos genéricos
     * (p. ej. {@code new GenericType<List<CourseRow>>(){}}).
     */
    public <T> T readOrThrow(Response response, GenericType<T> type) {
        if (isSuccessful(response)) {
            return response.readEntity(type);
        }
        throw toDomainException(response);
    }

    private static boolean isSuccessful(Response response) {
        return response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
    }

    /**
     * Mapea una respuesta no-2xx a {@link DomainException}. Intenta leer el
     * envelope estándar {@link ErrorResponse} para preservar el {@code code} y
     * la {@code metadata} de dominio del upstream; si el cuerpo no es el
     * envelope esperado, cae a una excepción genérica con el status HTTP.
     */
    private static DomainException toDomainException(Response response) {
        int status = response.getStatus();
        response.bufferEntity(); // permite reintentar la lectura si la primera falla
        try {
            ErrorResponse e = response.readEntity(ErrorResponse.class);
            if (e != null && (e.code() != null || e.message() != null)) {
                int upstreamStatus = e.status() != 0 ? e.status() : status;
                return new DomainException(upstreamStatus, e.code(), e.message())
                        .withAll(e.metadata());
            }
        } catch (RuntimeException ignored) {
            // El upstream no devolvió el envelope estándar — cae al genérico.
        }
        return new DomainException(status, null, "Upstream service returned HTTP " + status);
    }
}

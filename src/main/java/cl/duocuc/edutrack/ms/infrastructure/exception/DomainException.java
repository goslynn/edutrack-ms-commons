package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excepción base de cualquier error de dominio expresado por un microservicio.
 *
 * <p>A diferencia de {@link jakarta.ws.rs.WebApplicationException} (que sólo
 * lleva un status HTTP en su {@code Response}), una {@code DomainException}
 * acarrea todos los datos que necesita {@link GlobalExceptionMappers} para
 * construir el envelope {@link ErrorResponse}:</p>
 *
 * <ul>
 *   <li>{@link #status()} — código HTTP con el que se responde.</li>
 *   <li>{@link #code()} — código de dominio estable (convención
 *       {@code <MS>.<ENTIDAD>.<CONDICION>} en SCREAMING_SNAKE, ej.
 *       {@code "AUTH.USER.EMAIL_EXISTS"}). Pensado para que el cliente/UI
 *       discrimine condiciones sin parsear el mensaje, y se mantiene estable
 *       aunque el {@code message} cambie entre versiones.</li>
 *   <li>{@link #message()} ({@code getMessage()}) — texto legible para
 *       humanos. Puede internacionalizarse o cambiar entre versiones.</li>
 *   <li>{@link #metadata()} — pares clave/valor con contexto estructurado
 *       (por ejemplo {@code {"email": "foo@bar"}}). Se preserva el orden de
 *       inserción ({@link LinkedHashMap}) para que el JSON resultante sea
 *       reproducible.</li>
 * </ul>
 *
 * <h3>Uso típico</h3>
 * <p>En lugar de lanzar {@code WebApplicationException}, el dominio lanza una
 * de las subclases sugar ({@link ConflictException}, {@link NotFoundException},
 * {@link ForbiddenException}) y agrega contexto encadenado:</p>
 *
 * <pre>{@code
 * throw new ConflictException("AUTH.USER.EMAIL_EXISTS", "Email already in use")
 *     .with("email", email);
 * }</pre>
 *
 * <p>El {@link GlobalExceptionMappers#mapDomain handler global} la convierte en
 * un {@link ErrorResponse} con status, code, message, metadata, timestamp y
 * path. La traza solo se incluye si {@code edutrack.errors.expose-stacktrace=true}.</p>
 *
 * <p>Si el dominio necesita un status que no está cubierto por las subclases
 * sugar (p. ej. {@code 422}), puede instanciarse {@code DomainException}
 * directamente con el status como {@code int} o {@link Response.Status}.</p>
 */
public class DomainException extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    /**
     * @param status  código HTTP con el que se debe responder ({@code 4xx}/{@code 5xx})
     * @param code    código de dominio estable
     *                ({@code <MS>.<ENTIDAD>.<CONDICION>}); puede ser {@code null}
     *                si el caller no quiere exponer uno
     * @param message texto legible (se almacena como {@code getMessage()})
     */
    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** Variante que toma {@link Response.Status} en lugar de un {@code int}. */
    public DomainException(Response.Status status, String code, String message) {
        this(status.getStatusCode(), code, message);
    }

    /**
     * Variante que preserva la excepción causa (la {@code cause} se conserva
     * para logging pero <b>no</b> se serializa en el envelope JSON).
     */
    public DomainException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    /**
     * Agrega un par clave/valor a la metadata y devuelve {@code this} para
     * permitir encadenar el {@code throw}:
     *
     * <pre>{@code throw new NotFoundException(code, msg).with("userId", id);}</pre>
     *
     * <p>Si la clave ya existía, se reemplaza el valor. El orden de inserción
     * se preserva en la serialización JSON.</p>
     */
    public DomainException with(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Hace merge de todas las entradas del map en la metadata. Si una clave
     * coincide con una ya existente, la nueva la reemplaza. {@code null} es
     * un no-op.
     */
    public DomainException withAll(Map<String, Object> entries) {
        if (entries != null) metadata.putAll(entries);
        return this;
    }

    /** Status HTTP a responder. */
    public int status() { return status; }

    /** Código de dominio estable; puede ser {@code null}. */
    public String code() { return code; }

    /**
     * Vista <b>inmutable</b> de la metadata acumulada. El handler global la
     * incluye como atributo {@code metadata} del envelope cuando no está vacía.
     */
    public Map<String, Object> metadata() { return Collections.unmodifiableMap(metadata); }
}

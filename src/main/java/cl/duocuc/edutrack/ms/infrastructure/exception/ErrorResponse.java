package cl.duocuc.edutrack.ms.infrastructure.exception;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Envelope JSON único con el que cualquier microservicio responde un error.
 * Es el contrato visible para los clientes: cualquier excepción que escape de
 * un recurso pasa por {@link GlobalExceptionMappers} y se serializa con esta
 * forma.
 *
 * <p>Los campos opcionales ({@code code}, {@code metadata}, {@code trace})
 * se omiten cuando son {@code null} o vacíos gracias a
 * {@link JsonInclude#NON_EMPTY}, por lo que el envelope se mantiene mínimo
 * cuando no hay datos que reportar.</p>
 *
 * <h3>Forma típica del JSON</h3>
 * <pre>{@code
 * {
 *   "timestamp": "2026-05-20T14:32:11.123Z",
 *   "status": 409,
 *   "error": "Conflict",
 *   "code": "AUTH.USER.EMAIL_EXISTS",
 *   "message": "Email already in use",
 *   "path": "/auth/users",
 *   "metadata": { "email": "foo@bar.cl" }
 * }
 * }</pre>
 *
 * @param timestamp instante del error en el servidor, ISO-8601 UTC
 * @param status    código HTTP. Se incluye también en el body porque algunos
 *                  clientes pierden el status real al loguear o reenviar la
 *                  respuesta
 * @param error     <i>reason phrase</i> del status HTTP
 *                  (p. ej. {@code "Not Found"}); si el status no es estándar,
 *                  cae a {@code "HTTP <n>"}
 * @param code      código de dominio estable
 *                  ({@code <MS>.<ENTIDAD>.<CONDICION>}); puede ser {@code null}
 *                  cuando el error no proviene de {@link DomainException}
 * @param message   mensaje legible para humanos
 * @param path      path del request ({@code "/" + uriInfo.getPath()}); puede
 *                  ser {@code null} si no hay {@code UriInfo} disponible
 * @param metadata  contexto adicional estructurado; clave/valor arbitrarios
 *                  ({@code Map<String,Object>}); {@code null} cuando no hay
 * @param trace     hasta 25 frames del stack trace serializados como strings.
 *                  Solo se incluye cuando
 *                  {@code edutrack.errors.expose-stacktrace=true};
 *                  {@code null} en caso contrario
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    @JsonView(Views.Base.class) Instant timestamp,
    @JsonView(Views.Base.class) int status,
    @JsonView(Views.Base.class) String error,
    @JsonView(Views.Base.class) String code,
    @JsonView(Views.Base.class) String message,
    @JsonView(Views.Base.class) String path,
    @JsonView(Views.Base.class) Map<String, Object> metadata,
    @JsonView(Views.Base.class) List<String> trace
) {}

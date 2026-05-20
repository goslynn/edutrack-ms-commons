package cl.duocuc.edutrack.ms.infrastructure.context;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot inmutable de las cabeceras internas que el API Gateway propagó al
 * microservicio en el request actual, una vez parseadas y validadas. Se obtiene
 * vía {@link RequestContext#headers()}:
 *
 * <pre>{@code
 * @Inject RequestContext ctx;
 * ...
 * RequestHeaders h = ctx.headers();
 * UUID uid = h.requireUserId();
 * List<UUID> roles = h.roleIds();
 * }</pre>
 *
 * <p>Es un {@code record}, por lo que la instancia es inmutable y segura para
 * compartir entre componentes durante la ejecución del request. No se debe
 * inyectar directamente vía CDI: los records son {@code final} y no pueden
 * proxiarse; por eso el bean inyectable es {@link RequestContext}.</p>
 *
 * <h3>Contrato de valores</h3>
 * <ul>
 *   <li>Una cabecera <b>ausente</b> ⇒ {@link Optional#empty()} (para escalares)
 *       o lista vacía (para colecciones). Nunca {@code null}.</li>
 *   <li>Una cabecera <b>presente y bien formada</b> ⇒ valor tipado.</li>
 *   <li>Una cabecera <b>presente pero malformada</b> ⇒ depende del modo
 *       {@code edutrack.headers.validation.mode}:
 *       <ul>
 *         <li>{@link HeaderValidationMode#EAGER}: el request ya fue abortado
 *             con {@code 400} en {@link RequestContext}; este record no se
 *             llega a observar.</li>
 *         <li>{@link HeaderValidationMode#WARN}: el campo afectado se
 *             expone como ausente (mismo tratamiento que "no enviada").</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * @param userId  UUID del usuario autenticado si el Gateway lo propagó;
 *                {@link Optional#empty()} en caso contrario
 * @param roleIds UUIDs de los roles efectivos del usuario; lista inmutable,
 *                vacía si la cabecera no llegó o no contenía UUIDs válidos
 */
public record RequestHeaders(Optional<UUID> userId, List<UUID> roleIds) {

    /**
     * {@code true} si el Gateway propagó una identidad de usuario válida.
     * Equivale a {@code userId().isPresent()}; existe para legibilidad en
     * sitios de uso donde solo se quiere saber si hay alguien autenticado.
     */
    public boolean hasIdentity() {
        return userId.isPresent();
    }

    /**
     * Devuelve el UUID del usuario o lanza {@code 401 Unauthorized} si no
     * hay identidad propagada. La ausencia de identidad se modela como fallo
     * de autenticación, no de validación de datos: sin un usuario al que
     * atribuir la operación no tiene sentido continuar.
     *
     * <p>Se usa en endpoints donde la identidad es obligatoria pero no se
     * declaró {@code @RequirePermission}: la anotación cubre ese caso por
     * otra vía (el filtro aborta con 403 antes), aquí se cubre el caso de
     * lógica de negocio que necesita el {@code userId} explícitamente.</p>
     *
     * @throws WebApplicationException con status {@code 401} si {@link #userId()}
     *                                 está vacío
     */
    public UUID requireUserId() {
        return userId.orElseThrow(
            () -> new WebApplicationException(Response.Status.UNAUTHORIZED));
    }
}

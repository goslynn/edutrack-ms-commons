package cl.duocuc.edutrack.ms.infrastructure.discovery;

import cl.duocuc.edutrack.ms.infrastructure.context.InternalHeader;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Propaga la <b>identidad del request entrante</b> hacia las llamadas
 * inter-servicio salientes. Es la pieza que reemplaza el reenvío manual de
 * {@code X-User-Id}/{@code X-User-Roles} como {@code @HeaderParam} en cada
 * método de client: un client declarativo la engancha con
 * {@code @RegisterClientHeaders(IdentityHeadersFactory.class)} y los headers se
 * agregan solos en cada salida.
 *
 * <pre>{@code
 * @RegisterRestClient(configKey = ServiceIds.COURSE)
 * @RegisterClientHeaders(IdentityHeadersFactory.class)
 * @Path("/courses")
 * public interface CourseClient { ... }
 * }</pre>
 *
 * <h3>Fuente de la identidad</h3>
 * <p>Lee el {@link RequestContext} request-scoped —el mismo intérprete único de
 * cabeceras internas que usa todo el MS— y reenvía exactamente lo que el API
 * Gateway propagó. Los nombres de header salen de {@link InternalHeader} (única
 * fuente de verdad), nunca strings sueltos.</p>
 *
 * <h3>Ausencia de identidad</h3>
 * <p>Si el request no trae usuario ({@link RequestHeaders#userId()} vacío) no se
 * agrega ningún header: la llamada sale anónima y es el upstream quien decide si
 * la rechaza. Igual para roles vacíos.</p>
 *
 * <h3>Alcance</h3>
 * <p>Pensada para clients usados <b>dentro</b> del ciclo de un request HTTP
 * (que es cuando hay identidad que propagar). Inyecta el proxy request-scoped de
 * {@link RequestContext}; usarla fuera de un request activo (p. ej. un job
 * programado) no tiene identidad que reenviar por diseño.</p>
 */
@ApplicationScoped
public class IdentityHeadersFactory implements ClientHeadersFactory {

    @Inject
    RequestContext requestContext;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incoming,
                                                  MultivaluedMap<String, String> outgoing) {
        MultivaluedMap<String, String> propagated = new MultivaluedHashMap<>();
        RequestHeaders headers = requestContext.headers();

        headers.userId().ifPresent(
            id -> propagated.add(InternalHeader.USER_ID.wire, id.toString()));

        if (!headers.roleIds().isEmpty()) {
            String roles = headers.roleIds().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));
            propagated.add(InternalHeader.USER_ROLES.wire, roles);
        }
        return propagated;
    }
}

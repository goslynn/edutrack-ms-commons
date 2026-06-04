package cl.duocuc.edutrack.ms.infrastructure.security;

import cl.duocuc.edutrack.ms.clients.AuthClient;
import cl.duocuc.edutrack.ms.infrastructure.discovery.HTTPClientUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Implementación por defecto de {@link PermissionEvaluator}: delega la decisión
 * en el Auth Service vía HTTP usando {@link AuthClient}. Está anotada con
 * {@link DefaultBean @DefaultBean}, por lo que cualquier microservicio que
 * declare otra implementación {@code @ApplicationScoped} de
 * {@link PermissionEvaluator} en su classpath la sustituye sin tocar
 * configuración. El caso típico es el propio Auth Service, que evalúa los
 * permisos localmente contra {@code auth.role_permissions} sin pasar por HTTP.
 *
 * <h3>Por qué basta el endpoint {@code GET /auth/access}</h3>
 * <p>El {@code AccessResource} de Auth ya devuelve {@code effectiveFlags} como
 * el OR completo de los grants del usuario sobre el recurso <b>incluyendo el
 * comodín</b> {@link ResourceIds#ALL} (es el mismo cálculo que alimenta
 * {@code @RequirePermission}). El evaluador remoto, por tanto, no necesita un
 * segundo OR: una sola llamada trae los flags efectivos y el chequeo de bits se
 * hace localmente. No hace falta endpoint nuevo.</p>
 *
 * <h3>Identidad</h3>
 * <p>La identidad del usuario no viaja como parámetro: el {@code roleIds} que
 * recibe {@link #hasPermission} es el mismo que el filtro leyó del
 * {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestContext}, y la
 * llamada HTTP reenvía {@code X-User-Id}/{@code X-User-Roles} automáticamente
 * vía {@code @RegisterClientHeaders} del cliente. El parámetro {@code roleIds}
 * solo se usa para cortocircuitar el caso "usuario sin roles" y ahorrar la
 * llamada remota.</p>
 *
 * <h3>Fail-closed</h3>
 * <p>Cualquier excepción durante la llamada (timeout, 5xx, indisponibilidad de
 * Auth, error de deserialización) se loguea como {@code WARN} y se traduce a
 * {@code false}. Una falla intermitente de Auth nunca debe escalar a un
 * permiso concedido.</p>
 */
@ApplicationScoped
@DefaultBean
public class RemotePermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOG = Logger.getLogger(RemotePermissionEvaluator.class);

    @Inject
    @RestClient
    AuthClient client;

    @Inject
    HTTPClientUtils clientUtils;

    @Override
    public boolean hasPermission(List<UUID> roleIds, String resourceKey, short requiredBits) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        try {
            // La 'permission' enviada es irrelevante para el resultado: solo se
            // lee effectiveFlags (OR completo incl. comodín) y el chequeo de
            // bits se hace aquí. Se manda READ por ser el default del endpoint.
            Response raw = client.check(resourceKey, Permission.READ.name());
            AccessCheckResponse resp = clientUtils.readOrThrow(raw, AccessCheckResponse.class);
            return (resp.effectiveFlags() & requiredBits) == requiredBits;
        } catch (RuntimeException e) {
            LOG.warnf(e, "Fallo consultando /auth/access (resource=%s, bits=%d) — fail-closed",
                    resourceKey, requiredBits);
            return false;
        }
    }

    /**
     * Vista mínima del JSON emitido por {@code AccessResource} en {@code auth/}.
     * Solo declara {@code effectiveFlags}; el resto del payload se descarta con
     * {@link JsonIgnoreProperties}. El campo no necesita {@code @JsonView}: la
     * configuración Jackson de la librería ({@code JacksonCustomConfig}) deja
     * {@code DEFAULT_VIEW_INCLUSION} habilitado, así que una propiedad sin vista
     * se incluye en la vista activa por defecto ({@code Base}).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessCheckResponse(
            short effectiveFlags
    ) {}
}

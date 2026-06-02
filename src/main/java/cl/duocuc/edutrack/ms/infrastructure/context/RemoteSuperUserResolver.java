package cl.duocuc.edutrack.ms.infrastructure.context;

import cl.duocuc.edutrack.ms.clients.AuthAccessClient;
import cl.duocuc.edutrack.ms.infrastructure.discovery.HTTPClientUtils;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación por defecto de {@link SuperUserResolver}: pregunta al Auth
 * Service vía HTTP usando {@link AuthAccessClient}. Está anotada con
 * {@link DefaultBean @DefaultBean}, por lo que cualquier microservicio que
 * declare otra implementación {@code @ApplicationScoped} de
 * {@link SuperUserResolver} en su propio classpath la sustituye sin tocar
 * configuración (caso del propio Auth Service, que resuelve localmente).
 *
 * <h3>Algoritmo</h3>
 * <ol>
 *   <li>Si el request no trae {@code X-User-Id} ⇒ {@code false} sin llamadas
 *       remotas.</li>
 *   <li>Llama a {@code GET /auth/access?resourceUuid=ALL&permission=EXECUTE}
 *       reenviando las cabeceras de identidad del request actual. La
 *       {@code permission} pasada es irrelevante para el resultado: solo se
 *       lee {@code effectiveFlags} de la respuesta, que ya es el OR completo
 *       del usuario sobre el recurso comodín.</li>
 *   <li>Devuelve {@code (effectiveFlags & 7) == 7} — los tres bits
 *       {@code rwx} encendidos sobre {@link ResourceIds#ALL}.</li>
 * </ol>
 *
 * <h3>Fail-closed</h3>
 * <p>Cualquier excepción durante la llamada (timeout, 5xx, error de
 * deserialización, indisponibilidad del Auth Service) se loguea como
 * {@code WARN} y se traduce a {@code false}. Nunca se propaga al endpoint
 * llamante: una falla intermitente de Auth no debe escalar privilegios.</p>
 */
@ApplicationScoped
@DefaultBean
public class RemoteSuperUserResolver implements SuperUserResolver {

    private static final Logger LOG = Logger.getLogger(RemoteSuperUserResolver.class);

    private static final short RWX = 7;

    @Inject
    RequestContext requestContext;

    @Inject
    @RestClient
    AuthAccessClient client;

    @Inject
    HTTPClientUtils clientUtils;

    @Override
    public boolean isSuper() {
        RequestHeaders h = requestContext.headers();
        if (h.userId().isEmpty()) {
            return false;
        }
        String userId = h.userId().get().toString();
        try {
            var raw = client.check(ResourceIds.ALL, Permission.EXECUTE.name());
            var resp = clientUtils.readOrThrow(raw, AccessCheckResponse.class);
            return (resp.effectiveFlags() & RWX) == RWX;
        } catch (RuntimeException e) {
            LOG.warnf(e, "Fallo consultando /auth/access para super-check (userId=%s) — fail-closed", userId);
            return false;
        }
    }

    /**
     * Vista mínima del JSON emitido por {@code AccessResource} en
     * {@code auth/}. Solo declara los campos que esta librería necesita
     * ({@code effectiveFlags}); el resto del payload se descarta con
     * {@link JsonIgnoreProperties}.
     *
     * <p>El campo se anota con {@link JsonView} porque la configuración
     * Jackson de la librería ({@code JacksonCustomConfig}) deshabilita
     * {@code DEFAULT_VIEW_INCLUSION}: sin la vista, Jackson no
     * deserializaría el campo.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessCheckResponse(
            short effectiveFlags
    ) {}

}

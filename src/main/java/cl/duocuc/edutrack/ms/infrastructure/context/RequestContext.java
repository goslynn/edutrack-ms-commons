package cl.duocuc.edutrack.ms.infrastructure.context;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Único intérprete de las cabeceras internas que el API Gateway propaga al
 * microservicio. Es el punto de entrada que el resto del código debe usar
 * para leer la identidad del request: nunca {@code @HeaderParam("X-...")} ni
 * {@code routingContext.request().getHeader("X-...")} en endpoints o servicios.
 *
 * <h3>Ciclo de vida</h3>
 * <p>Bean {@code @RequestScoped} y proxyable. En {@link PostConstruct} parsea
 * cada cabecera del catálogo {@link InternalHeader} una sola vez y arma el
 * record {@link RequestHeaders}. Inyecciones sucesivas durante el mismo request
 * obtienen la misma instancia (sin volver a parsear), por lo que el costo es
 * fijo y predecible.</p>
 *
 * <pre>{@code
 * @Inject RequestContext ctx;
 * ...
 * UUID uid = ctx.headers().requireUserId();
 * List<UUID> roles = ctx.headers().roleIds();
 * }</pre>
 *
 * <h3>Reglas de interpretación por cabecera</h3>
 * <ol>
 *   <li><b>Presente y bien formada</b> ⇒ valor tipado en el record.</li>
 *   <li><b>Ausente o en blanco (solo whitespace)</b> ⇒ valor vacío
 *       ({@link Optional#empty()} / lista vacía). No es fallo de validación.</li>
 *   <li><b>Presente pero malformada</b> ⇒ política configurable vía
 *       {@code edutrack.headers.validation.mode}:
 *       <ul>
 *         <li>{@link HeaderValidationMode#EAGER} (default): aborta con
 *             {@code 400 Bad Request} y mensaje
 *             {@code "Cabecera interna malformada: <wire>"}.</li>
 *         <li>{@link HeaderValidationMode#WARN}: loguea {@code WARN} con el
 *             nombre y el valor recibido y trata la cabecera como ausente.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h3>Detalle de parsing por cabecera</h3>
 * <ul>
 *   <li>{@link InternalHeader#USER_ID}: se parsea con
 *       {@link UUID#fromString(String)}. Excepción del parser ⇒ política de
 *       malformado.</li>
 *   <li>{@link InternalHeader#USER_ROLES}: se separa por coma, se hace
 *       {@code trim} a cada token, se descartan los tokens vacíos y se parsea
 *       cada uno como UUID. Basta con que <b>un</b> token sea inválido para
 *       que toda la lista se considere malformada (los tokens previos no se
 *       conservan): la implementación devuelve lista vacía en modo WARN o
 *       aborta el request en modo EAGER.</li>
 * </ul>
 *
 * <h3>Dependencias</h3>
 * <p>Inyecta el {@link RoutingContext} de Vert.x (provisto por Quarkus REST)
 * para leer headers crudos. La propiedad
 * {@code edutrack.headers.validation.mode} se inyecta vía
 * {@link ConfigProperty} con default {@code EAGER}.</p>
 */
@RequestScoped
public class RequestContext {

    private static final Logger LOG = Logger.getLogger(RequestContext.class);

    @Inject
    RoutingContext routingContext;

    @ConfigProperty(name = "edutrack.headers.validation.mode", defaultValue = "EAGER")
    HeaderValidationMode mode;

    @Inject
    SuperUserResolver superUserResolver;

    private RequestHeaders headers;
    private Boolean superCached;

    @PostConstruct
    void interpret() {
        headers = new RequestHeaders(parseUserId(), parseRoleIds());
    }

    /** Vista inmutable, ya interpretada y validada, de las cabeceras del request. */
    public RequestHeaders headers() {
        return headers;
    }

    /**
     * Indica si la identidad propagada por el Gateway es <b>superusuaria</b>,
     * según la definición operacional de EduTrack: posee {@code rwx} sobre
     * {@link cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds#ALL}.
     *
     * <p>Delega la evaluación en el {@link SuperUserResolver} resuelto por
     * CDI ({@link RemoteSuperUserResolver} por defecto vía HTTP al Auth
     * Service; cada MS puede sustituirlo por una implementación local). El
     * resultado se memoiza para el resto del request: llamadas sucesivas en
     * el mismo request no vuelven a invocar al resolver.</p>
     *
     * <p>Cualquier fallo del resolver se traduce a {@code false} (fail-closed)
     * dentro de la propia implementación: este método no lanza.</p>
     */
    public boolean isSuper() {
        Boolean cached = superCached;
        if (cached == null) {
            cached = superUserResolver.isSuper();
            superCached = cached;
        }
        return cached;
    }

    private Optional<UUID> parseUserId() {
        String raw = header(InternalHeader.USER_ID);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return onMalformed(InternalHeader.USER_ID, raw, Optional.empty());
        }
    }

    private List<UUID> parseRoleIds() {
        String raw = header(InternalHeader.USER_ROLES);
        if (raw == null) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(t));
            } catch (IllegalArgumentException e) {
                return onMalformed(InternalHeader.USER_ROLES, raw, List.of());
            }
        }
        return List.copyOf(ids);
    }

    /** Header con trim; {@code null} si está ausente o en blanco. */
    private String header(InternalHeader h) {
        String v = routingContext.request().getHeader(h.wire);
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Política de header malformado. En {@link HeaderValidationMode#EAGER}
     * aborta el request con {@code 400}; en {@link HeaderValidationMode#WARN}
     * loguea y devuelve el valor vacío {@code fallback}.
     */
    private <T> T onMalformed(InternalHeader h, String raw, T fallback) {
        if (mode == HeaderValidationMode.EAGER) {
            throw new WebApplicationException(
                "Cabecera interna malformada: " + h.wire,
                Response.Status.BAD_REQUEST);
        }
        LOG.warnf("Cabecera interna malformada %s=%s — se ignora (modo WARN)", h.wire, raw);
        return fallback;
    }
}

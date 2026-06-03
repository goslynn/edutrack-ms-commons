package cl.duocuc.edutrack.ms.clients;

import cl.duocuc.edutrack.ms.infrastructure.context.RemoteSuperUserResolver;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import cl.duocuc.edutrack.ms.infrastructure.context.SuperUserResolver;
import cl.duocuc.edutrack.ms.infrastructure.discovery.IdentityHeadersFactory;
import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client tipado contra el endpoint {@code GET /auth/access} del Auth
 * Service, consumido por {@link RemoteSuperUserResolver} (y disponible para
 * cualquier otro componente de la librería que necesite delegar la decisión de
 * permisos en Auth).
 *
 * <h3>Configuración del cliente</h3>
 * <p>Sigue el {@linkplain cl.duocuc.edutrack.ms.infrastructure.discovery
 * estándar de comunicación inter-servicio} de la plataforma: client
 * <b>declarativo</b> con {@code @RegisterRestClient(configKey = ServiceIds.AUTH)}.
 * Su URL ({@code quarkus.rest-client.auth.url}) no se escribe a mano: la deriva
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.DiscoveryConfigSourceFactory}
 * del patrón {@code edutrack.discovery.pattern}, resuelto por DNS de Fly.io
 * ({@code edutrack-auth.fly.internal}). Se inyecta con
 * {@code @Inject @RestClient AuthAccessClient}; un MS consumidor no necesita
 * declarar nada (solo el override de {@code edutrack.discovery.pattern} en
 * local).</p>
 * <p>No usa {@code @RegisterClientHeaders}: la identidad se pasa como
 * {@link HeaderParam} explícitos para que la firma sea autoexplicativa (ver
 * {@link RemoteSuperUserResolver}), justamente el caso en que el reenvío manual
 * de cabeceras es preferible al automático.</p>
 * <p>Este es un caso límite legítimo dentro de {@code infrastructure}: la
 * verificación de permisos es authz transversal, no dominio de Auth. Un client
` * que traiga datos de negocio de un MS va en {@code clients.<servicio>}.</p>
 * <p>El cliente solo se instancia cuando alguien lo inyecta: en
 * microservicios que aporten su propia implementación de
 * {@link SuperUserResolver} (típicamente el propio Auth Service) nunca se
 * crea ni se abre conexión hacia Auth.</p>
 *
 * <h3>Identidad del request</h3>
 * <p>El endpoint {@code /auth/access} evalúa permisos sobre la identidad
 * propagada por el API Gateway: el cliente debe reenviar
 * {@code X-User-Id} y {@code X-User-Roles} obtenidos del
 * {@link RequestContext}. Se pasan como {@link HeaderParam} explícitos para
 * que la firma sea autoexplicativa y para no depender de filtros globales del
 * lado del cliente.</p>
 *
 */
@RegisterRestClient(configKey = ServiceIds.AUTH)
@RegisterClientHeaders(IdentityHeadersFactory.class)
public interface AuthAccessClient {

    /**
     * Consulta los flags efectivos del usuario propagado sobre un recurso.
     *
     * @param resourceKey  clave estable del recurso a evaluar; usar
     *                     {@link ResourceIds#ALL} para preguntar por el comodín
     *                     (caso del check de superusuario).
     * @param permission   permiso requerido para que el endpoint compute
     *                     {@code allowed}; no afecta a {@code effectiveFlags},
     *                     que es siempre el OR completo del usuario sobre el
     *                     recurso (incluyendo el comodín).
     * @return DTO con los flags efectivos. El consumidor decide qué hacer con
     *         ellos (p. ej. {@code (effectiveFlags & 7) == 7} para "super").
     */
    @GET
    @Path("/auth/access")
    @Produces(MediaType.APPLICATION_JSON)
    Response check(@QueryParam("resourceKey") String resourceKey, @QueryParam("permission") String permission);

}

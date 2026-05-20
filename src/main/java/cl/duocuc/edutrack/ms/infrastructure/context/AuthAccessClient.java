package cl.duocuc.edutrack.ms.infrastructure.context;

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
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client tipado contra el endpoint {@code GET /auth/access} del Auth
 * Service, consumido por {@link RemoteSuperUserResolver} (y disponible para
 * cualquier otro componente de la librería que necesite delegar la decisión de
 * permisos en Auth).
 *
 * <h3>Configuración del cliente</h3>
 * <p>El cliente sigue el {@linkplain cl.duocuc.edutrack.ms.infrastructure.discovery
 * estándar de comunicación inter-servicio} de la plataforma: la URL base es
 * {@code "stork://" + }{@link ServiceIds#AUTH}, fijada directamente como
 * {@code baseUri} de {@link RegisterRestClient}. El service-id se resuelve en
 * runtime por Stork contra Consul usando los defaults globales declarados en
 * el {@code META-INF/microprofile-config.properties} de esta librería; un MS
 * consumidor no necesita declarar nada de Stork ni la URL del cliente —
 * únicamente {@code edutrack.consul.host} / {@code edutrack.consul.port} si
 * los defaults no aplican a su ambiente.</p>
 * <p>El cliente solo se instancia cuando alguien lo inyecta: en
 * microservicios que aporten su propia implementación de
 * {@link SuperUserResolver} (típicamente el propio Auth Service) nunca se
 * crea y la conexión con Consul no se abre.</p>
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
@RegisterRestClient(baseUri = "stork://" + ServiceIds.AUTH, configKey = "edutrack-auth-access")
public interface AuthAccessClient {

    /**
     * Consulta los flags efectivos del usuario propagado sobre un recurso.
     *
     * @param userId       valor de {@code X-User-Id} (UUID textual).
     * @param userRoles    valor de {@code X-User-Roles} (CSV de UUIDs).
     * @param resourceUuid recurso a evaluar; usar
     *                     {@link ResourceIds#ALL_UUID} para preguntar por el
     *                     comodín (caso del check de superusuario).
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
    AccessCheckResponse check(
        @HeaderParam("X-User-Id") String userId,
        @HeaderParam("X-User-Roles") String userRoles,
        @QueryParam("resourceUuid") String resourceUuid,
        @QueryParam("permission") String permission);

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
        @JsonView(Views.Base.class) short effectiveFlags
    ) {}
}

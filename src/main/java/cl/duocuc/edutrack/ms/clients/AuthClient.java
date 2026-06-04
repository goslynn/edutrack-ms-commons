package cl.duocuc.edutrack.ms.clients;

import cl.duocuc.edutrack.ms.infrastructure.discovery.IdentityHeadersFactory;
import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

/**
 * REST Client tipado que cubre <b>la totalidad</b> de la API del Auth Service
 * (autenticación, verificación de acceso, usuarios, roles, asignación de roles,
 * permisos por rol y JWKS). Es el SDK de consumo para cualquier otro
 * microservicio de EduTrack que necesite hablar con Auth.
 *
 * <h3>Configuración del cliente</h3>
 * <p>Sigue el {@linkplain cl.duocuc.edutrack.ms.infrastructure.discovery
 * estándar de comunicación inter-servicio} de la plataforma: client
 * <b>declarativo</b> con {@code @RegisterRestClient(configKey = ServiceIds.AUTH)}.
 * Su URL ({@code quarkus.rest-client.auth.url}) no se escribe a mano: la deriva
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.DiscoveryConfigSourceFactory}
 * del patrón {@code edutrack.discovery.pattern}, resuelto por DNS de Fly.io
 * ({@code edutrack-auth.fly.internal}). Se inyecta con
 * {@code @Inject @RestClient AuthClient}; un MS consumidor no necesita declarar
 * nada (solo el override de {@code edutrack.discovery.pattern} en local).</p>
 *
 * <h3>Contrato {@code Response}/{@code Object}</h3>
 * <p>Todos los métodos retornan {@link Response} crudo y los cuerpos de request
 * viajan como {@link Object}: el client se mantiene <b>genérico</b> y
 * tolerant-reader, sin acoplarse a los DTOs de dominio de Auth (que viven en su
 * propio MS y no son visibles desde la librería). El consumidor decide cómo
 * leer el cuerpo — típicamente con
 * {@link cl.duocuc.edutrack.ms.infrastructure.discovery.HTTPClientUtils#readOrThrow}
 * para reconstruir la {@code DomainException} del upstream, o
 * {@code readEntity(...)} contra su propio DTO tolerant-reader.</p>
 *
 * <h3>Identidad del request</h3>
 * <p>{@code @RegisterClientHeaders(IdentityHeadersFactory.class)} reenvía
 * automáticamente {@code X-User-Id}/{@code X-User-Roles} desde el
 * {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestContext}, de modo
 * que los endpoints protegidos de Auth (usuarios, roles, permisos) evalúan
 * {@code @RequirePermission} sobre la identidad propagada por el Gateway. Las
 * llamadas son directas app-a-app vía DNS de Fly.io, <b>no</b> pasan por el
 * Gateway.</p>
 *
 * <p>Todas las rutas se prefijan con {@code "/"+ServiceIds.AUTH} porque el Auth
 * Service monta su {@code @ApplicationPath} bajo {@code /auth} (primer segmento
 * del path = nombre de la app, contrato del Gateway).</p>
 */
@RegisterRestClient(configKey = ServiceIds.AUTH)
@RegisterClientHeaders(IdentityHeadersFactory.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthClient {

    /** {@code POST /auth/login} — autentica con credenciales y emite tokens. */
    @POST
    @Path("/" + ServiceIds.AUTH + "/login")
    Response login(Object request);

    /** {@code POST /auth/refresh} — renueva el access token con un refresh token. */
    @POST
    @Path("/" + ServiceIds.AUTH + "/refresh")
    Response refresh(Object request);

    /** {@code POST /auth/logout} — revoca las sesiones de la identidad propagada. */
    @POST
    @Path("/" + ServiceIds.AUTH + "/logout")
    Response logout();


    /**
     * {@code GET /auth/access} — flags efectivos de la identidad propagada sobre
     * un recurso. Usar {@link ResourceIds#ALL} en {@code resourceKey} para
     * preguntar por el comodín (check de superusuario). {@code permission} es el
     * nombre del enum {@code Permission} ({@code READ}/{@code WRITE}/
     * {@code EXECUTE}); no altera {@code effectiveFlags}, que es siempre el OR
     * completo del usuario sobre el recurso.
     */
    @GET
    @Path("/" + ServiceIds.AUTH + "/access")
    Response check(@QueryParam("resourceKey") String resourceKey,
                   @QueryParam("permission") String permission);


    /** {@code GET /auth/users} — lista todos los usuarios. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/users")
    Response listUsers();

    /** {@code POST /auth/users} — crea un usuario. */
    @POST
    @Path("/" + ServiceIds.AUTH + "/users")
    Response createUser(Object request);

    /** {@code GET /auth/users/{id}} — obtiene un usuario por id. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/users/{id}")
    Response getUser(@PathParam("id") UUID id);

    /** {@code PUT /auth/users/{id}} — actualiza un usuario. */
    @PUT
    @Path("/" + ServiceIds.AUTH + "/users/{id}")
    Response updateUser(@PathParam("id") UUID id, Object request);

    /** {@code DELETE /auth/users/{id}} — deshabilita un usuario. */
    @DELETE
    @Path("/" + ServiceIds.AUTH + "/users/{id}")
    Response disableUser(@PathParam("id") UUID id);

    /** {@code DELETE /auth/users/{id}/sessions} — revoca todas las sesiones del usuario. */
    @DELETE
    @Path("/" + ServiceIds.AUTH + "/users/{id}/sessions")
    Response revokeUserSessions(@PathParam("id") UUID id);

    /** {@code GET /auth/users/{userId}/roles} — roles asignados a un usuario. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/users/{userId}/roles")
    Response listUserRoles(@PathParam("userId") UUID userId);

    /** {@code POST /auth/users/{userId}/roles/{roleId}} — asigna un rol a un usuario. */
    @POST
    @Path("/" + ServiceIds.AUTH + "/users/{userId}/roles/{roleId}")
    Response assignRole(@PathParam("userId") UUID userId, @PathParam("roleId") UUID roleId);

    /** {@code DELETE /auth/users/{userId}/roles/{roleId}} — revoca un rol de un usuario. */
    @DELETE
    @Path("/" + ServiceIds.AUTH + "/users/{userId}/roles/{roleId}")
    Response revokeRole(@PathParam("userId") UUID userId, @PathParam("roleId") UUID roleId);


    /** {@code GET /auth/roles} — lista todos los roles. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/roles")
    Response listRoles();

    /** {@code POST /auth/roles} — crea un rol. */
    @POST
    @Path("/" + ServiceIds.AUTH + "/roles")
    Response createRole(Object request);

    /** {@code GET /auth/roles/{id}} — obtiene un rol por id. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/roles/{id}")
    Response getRole(@PathParam("id") UUID id);

    /** {@code PUT /auth/roles/{id}} — actualiza un rol. */
    @PUT
    @Path("/" + ServiceIds.AUTH + "/roles/{id}")
    Response updateRole(@PathParam("id") UUID id, Object request);

    /** {@code DELETE /auth/roles/{id}} — elimina un rol. */
    @DELETE
    @Path("/" + ServiceIds.AUTH + "/roles/{id}")
    Response deleteRole(@PathParam("id") UUID id);


    /** {@code GET /auth/roles/{roleId}/permissions} — permisos declarados de un rol. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/roles/{roleId}/permissions")
    Response listRolePermissions(@PathParam("roleId") UUID roleId);

    /** {@code PUT /auth/roles/{roleId}/permissions/{resourceKey}} — crea o actualiza el grant de un rol sobre un recurso. */
    @PUT
    @Path("/" + ServiceIds.AUTH + "/roles/{roleId}/permissions/{resourceKey}")
    Response upsertRolePermission(@PathParam("roleId") UUID roleId,
                                  @PathParam("resourceKey") String resourceKey,
                                  Object request);

    /** {@code DELETE /auth/roles/{roleId}/permissions/{resourceKey}} — elimina el grant de un rol sobre un recurso. */
    @DELETE
    @Path("/" + ServiceIds.AUTH + "/roles/{roleId}/permissions/{resourceKey}")
    Response deleteRolePermission(@PathParam("roleId") UUID roleId,
                                  @PathParam("resourceKey") String resourceKey);

    /** {@code GET /auth/roles/{roleId}/permissions/effective} — flags efectivos del rol sobre un recurso. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/roles/{roleId}/permissions/effective")
    Response effectiveRolePermission(@PathParam("roleId") UUID roleId,
                                     @QueryParam("resourceKey") String resourceKey);


    /** {@code GET /auth/.well-known/jwks.json} — set público de claves para validar JWT. */
    @GET
    @Path("/" + ServiceIds.AUTH + "/.well-known/jwks.json")
    Response jwks();
}

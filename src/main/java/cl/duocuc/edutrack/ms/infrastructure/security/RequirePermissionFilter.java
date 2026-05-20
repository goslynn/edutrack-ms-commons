package cl.duocuc.edutrack.ms.infrastructure.security;

import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Filtro JAX-RS que materializa la anotación {@link RequirePermission}.
 *
 * <h3>Activación</h3>
 * <p>La clase está anotada a su vez con {@link RequirePermission}: por las
 * reglas de <i>name binding</i> de JAX-RS, eso provoca que el filtro solo se
 * invoque sobre endpoints que también porten {@link RequirePermission}
 * (directamente o vía su clase contenedora). Endpoints sin la anotación no
 * pagan el costo del filtro. La anotación que la clase del filtro lleva
 * (recurso {@link ResourceIds#ALL_UUID}, {@link Permission#READ}) <b>no</b> se
 * evalúa contra los permisos del usuario: existe únicamente como marca de
 * binding y nunca se lee en {@link #filter(ContainerRequestContext)}.</p>
 *
 * <h3>Prioridad</h3>
 * <p>{@link Priorities#AUTHORIZATION} hace que se ejecute después de
 * autenticación y antes que filtros con prioridad mayor (deserialización,
 * Bean Validation). Para el desarrollador eso significa que el body no se
 * leerá hasta que el filtro haya autorizado el request — un {@code 403} no
 * cuesta el costo de parsear payloads.</p>
 *
 * <h3>Algoritmo por request</h3>
 * <ol>
 *   <li>Obtiene la anotación {@link RequirePermission}: primero del método del
 *       recurso; si no está, de la clase. Si tampoco está (no debería pasar
 *       por el name binding), el filtro hace early return.</li>
 *   <li>Inyecta {@link RequestContext} para leer la identidad del usuario y
 *       los roles propagados por el Gateway (ya parseados y validados).</li>
 *   <li>Si {@link RequirePermission#selfParam()} está definido y el usuario
 *       autenticado tiene identidad, compara {@code userId.toString()} contra
 *       el path-param indicado. Si coincide, autoriza sin consultar permisos.</li>
 *   <li>Parsea {@link RequirePermission#resource()} como {@link UUID} y delega
 *       a {@link PermissionEvaluator#hasPermission}.</li>
 *   <li>Si el evaluador devuelve {@code false}, aborta con
 *       {@code 403 Forbidden} y body vacío.</li>
 * </ol>
 *
 * <p>El cálculo de flags efectivos (incluido el OR con el comodín
 * {@link ResourceIds#ALL}) es responsabilidad de la implementación CDI de
 * {@link PermissionEvaluator}: el filtro no conoce el dominio del MS.</p>
 *
 * <h3>Pre-requisitos para que funcione</h3>
 * <ul>
 *   <li>Existe un bean CDI que implemente {@link PermissionEvaluator}.</li>
 *   <li>El bean {@link RequestContext} está disponible (lo está siempre que la
 *       librería esté en el classpath: es {@code @RequestScoped} y se inyecta
 *       solo).</li>
 *   <li>El Gateway propaga las cabeceras {@code X-User-Id} y
 *       {@code X-User-Roles} con UUIDs válidos. Si no hay identidad, los
 *       endpoints sin {@code selfParam} simplemente cae en
 *       {@code PermissionEvaluator} con {@code roleIds = []}.</li>
 * </ul>
 */
@Provider
@RequirePermission(resource = ResourceIds.ALL_UUID, value = Permission.READ)
@Priority(Priorities.AUTHORIZATION)
public class RequirePermissionFilter implements ContainerRequestFilter {

    @Inject
    PermissionEvaluator permissionEvaluator;

    @Inject
    RequestContext requestContext;

    @Context
    ResourceInfo resourceInfo;

    @Context
    UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext ctx) {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) return;

        RequirePermission ann = method.getAnnotation(RequirePermission.class);
        if (ann == null) {
            ann = resourceInfo.getResourceClass().getAnnotation(RequirePermission.class);
        }
        if (ann == null) return;

        RequestHeaders headers = requestContext.headers();

        // Excepción "self": el dueño del recurso accede sin chequear permisos.
        if (!ann.selfParam().isEmpty() && headers.hasIdentity()) {
            String pathVal = uriInfo.getPathParameters().getFirst(ann.selfParam());
            if (headers.userId().isPresent()
                    && headers.userId().get().toString().equals(pathVal)) {
                return;
            }
        }

        UUID resourceUuid = UUID.fromString(ann.resource());
        if (!permissionEvaluator.hasPermission(headers.roleIds(), resourceUuid, ann.value().bit)) {
            abort(ctx);
        }
    }

    private static void abort(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }
}

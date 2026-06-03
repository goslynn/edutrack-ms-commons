package cl.duocuc.edutrack.ms.infrastructure.context;

import cl.duocuc.edutrack.ms.infrastructure.security.PermissionEvaluator;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementación por defecto de {@link SuperUserResolver}: deriva la condición
 * "super" del mismo {@link PermissionEvaluator} resuelto por CDI, sin volver a
 * hablar con Auth por su cuenta. Está anotada con {@link DefaultBean @DefaultBean},
 * por lo que cualquier microservicio que declare otra implementación
 * {@code @ApplicationScoped} de {@link SuperUserResolver} la sustituye sin tocar
 * configuración (caso del propio Auth Service, que resuelve localmente).
 *
 * <h3>Definición operacional</h3>
 * <p>Ser "super" es tener los tres bits {@code rwx} sobre el recurso comodín
 * {@link ResourceIds#ALL}. Esto se reduce exactamente a una consulta de permiso
 * más: {@code evaluator.hasPermission(roleIds, ALL, rwx)}. Delegar evita
 * duplicar la lógica de bits y el cliente HTTP: en un MS remoto el evaluador es
 * {@link cl.duocuc.edutrack.ms.infrastructure.security.RemotePermissionEvaluator}
 * (una llamada a {@code GET /auth/access}); en Auth es la implementación local
 * contra la base de datos. El resolver no necesita saber cuál.</p>
 *
 * <h3>Fail-closed</h3>
 * <p>El manejo de fallos (timeout, indisponibilidad de Auth, etc.) vive en el
 * {@link PermissionEvaluator}, que ya es fail-closed: ante cualquier problema
 * responde {@code false}. Sin identidad propagada, {@code roleIds} viene vacío y
 * el evaluador resuelve {@code false} sin llamadas remotas.</p>
 *
 * <h3>Caché</h3>
 * <p>El resultado lo memoiza {@link RequestContext} a nivel de request, así que
 * esta implementación no cachea internamente.</p>
 */
@ApplicationScoped
@DefaultBean
public class RemoteSuperUserResolver implements SuperUserResolver {

    /** Los tres bits Unix-style ({@code rwx}) que definen al superusuario. */
    private static final short RWX = 7;

    @Inject
    RequestContext requestContext;

    @Inject
    PermissionEvaluator permissionEvaluator;

    @Override
    public boolean isSuper() {
        return permissionEvaluator.hasPermission(
                requestContext.headers().roleIds(), ResourceIds.ALL, RWX);
    }
}

package cl.duocuc.edutrack.ms.infrastructure.security;

import java.util.List;
import java.util.UUID;

/**
 * Punto de extensión CDI mediante el cual cada microservicio expone su lógica
 * de evaluación de permisos Unix-style. El
 * {@link RequirePermissionFilter filtro de autorización} consume este contrato
 * y no conoce ningún detalle del dominio: dónde viven los grants, cómo se
 * persisten o si vienen de un servicio remoto es decisión de la implementación.
 *
 * <p>Cada MS consumidor de la librería debe publicar exactamente <b>una</b>
 * implementación como bean CDI (típicamente {@code @ApplicationScoped}). El
 * Auth Service la implementa consultando la tabla {@code auth.role_permissions};
 * el resto de los MS suelen implementarla llamando al endpoint
 * {@code GET /auth/access} del Auth Service.</p>
 *
 * <h3>Algoritmo a implementar</h3>
 * <pre>{@code
 * effective = flags(roleIds, resourceKey) | flags(roleIds, ResourceIds.ALL)
 * return (effective & requiredBits) == requiredBits
 * }</pre>
 *
 * <p>Es decir, la implementación <b>debe</b> incluir el OR con el comodín
 * {@link ResourceIds#ALL}: un grant sobre el recurso comodín otorga ese permiso
 * sobre cualquier recurso concreto.</p>
 */
public interface PermissionEvaluator {

    /**
     * Indica si el conjunto de roles propagado por el Gateway satisface los
     * bits requeridos sobre el recurso solicitado.
     *
     * @param roleIds       UUIDs de rol del usuario autenticado tal como
     *                      vienen en
     *                      {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders#roleIds()}.
     *                      Una lista vacía es válida y normalmente resuelve a
     *                      {@code false} (usuario sin roles).
     * @param resourceKey   clave estable del recurso sobre el que se evalúa. En
     *                      las llamadas del filtro nunca es
     *                      {@link ResourceIds#ALL} directamente: el comodín solo
     *                      participa internamente en el OR de flags efectivos.
     * @param requiredBits  Bit(s) mínimos requeridos
     *                      ({@link Permission#bit Permission.X.bit}). El filtro
     *                      envía un único bit por anotación; la implementación
     *                      debe soportar combinaciones de bits por composición.
     * @return {@code true} si el usuario tiene el permiso; {@code false} en
     *         caso contrario.
     */
    boolean hasPermission(List<UUID> roleIds, String resourceKey, short requiredBits);
}

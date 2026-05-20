package cl.duocuc.edutrack.ms.infrastructure.security;

import java.util.UUID;

/**
 * Constantes transversales del modelo de permisos. Cada microservicio define
 * además su propio catálogo de {@code resource_uuid}s opacos para los recursos
 * de su dominio; lo que vive aquí son los valores comunes a todos.
 *
 * <p>{@link #ALL} es un <b>recurso comodín</b>: un grant de flags sobre este
 * UUID se interpreta como "permisos sobre cualquier recurso, presente o
 * futuro". La {@link PermissionEvaluator implementación} de cada MS debe hacer
 * el OR entre los flags efectivos del recurso solicitado y los flags efectivos
 * sobre {@code ALL} antes de comparar contra el mínimo requerido:</p>
 *
 * <pre>{@code effective = flags(role, resource) | flags(role, ALL)}</pre>
 *
 * <p>El comodín se usa al consultar, no al anotar endpoints: ningún recurso
 * REST debería declarar {@code @RequirePermission(resource = ALL_UUID, ...)}
 * como contrato de su API.</p>
 */
public final class ResourceIds {

    /**
     * Representación textual del UUID comodín
     * ({@code "00000000-0000-0000-0000-000000000000"}). Se expone como
     * {@link String} porque las anotaciones Java solo admiten constantes de
     * compilación: la versión {@link UUID} no puede usarse como valor de
     * {@link RequirePermission#resource()}.
     */
    public static final String ALL_UUID = "00000000-0000-0000-0000-000000000000";

    /**
     * UUID comodín ya tipado. Útil para comparar/almacenar en código que ya
     * trabaja con {@link UUID} (evaluadores, repositorios, persistencia).
     */
    public static final UUID ALL = UUID.fromString(ALL_UUID);

    private ResourceIds() {}
}

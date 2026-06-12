package cl.duocuc.edutrack.ms.infrastructure.security;

/**
 * Constantes transversales del modelo de permisos. Cada microservicio define
 * además su propio catálogo de <em>resource keys</em> (claves estables de
 * texto) para los recursos de su dominio; lo que vive aquí son los valores
 * comunes a todos.
 *
 * <p>Una <em>resource key</em> es un identificador legible y estable
 * ({@code "<servicio>.<recurso>"}, p. ej. {@code "auth.users"}) que nombra un
 * recurso de forma idéntica en todos los servicios. Es opaca para Auth: solo
 * se compara por igualdad. Al ser el mismo string en ambos lados de un grant,
 * el string <b>es</b> el contrato — no hay UUIDs que coordinar.</p>
 *
 * <p>{@link #ALL} es un <b>recurso comodín</b>: un grant de flags sobre esta
 * clave se interpreta como "permisos sobre cualquier recurso, presente o
 * futuro". La {@link PermissionEvaluator implementación} de cada MS debe hacer
 * el OR entre los flags efectivos del recurso solicitado y los flags efectivos
 * sobre {@code ALL} antes de comparar contra el mínimo requerido:</p>
 *
 * <pre>{@code effective = flags(role, resourceKey) | flags(role, ALL)}</pre>
 *
 * <p>El comodín se usa al consultar, no al anotar endpoints: ningún recurso
 * REST debería declarar {@code @RequirePermission(resource = ResourceIds.ALL, ...)}
 * como contrato de su API.</p>
 */
public final class ResourceIds {

    /**
     * Clave comodín reservada ({@code "*"}). Se expone como {@link String}
     * porque tanto las anotaciones Java (que solo admiten constantes de
     * compilación) como el almacenamiento del grant trabajan con la clave de
     * texto. No puede colisionar con una clave de recurso real.
     */
    public static final String ALL = "*";

    // TODO(resource-catalog): catálogo de recursos descontralizado (decisión: opción 3).
    //  Auth es deliberadamente opaco a las resource keys de otros dominios, así que NO
    //  centralizamos un catálogo en Auth ni una tabla `resources`. En su lugar, cada MS
    //  expone sus propias claves vía un endpoint de metadatos (p. ej. GET /<servicio>/meta/resources)
    //  derivado de su catálogo en código (AuthResourceId, CourseResourceId, ...). El frontend
    //  (o un BFF) agrega los catálogos barriendo ServiceIds.ALL para poblar la UI de
    //  administración de permisos por rol — hoy GET /roles/{id}/permissions solo devuelve los
    //  grants existentes, no el universo de recursos asignables. Pendiente: contrato común del
    //  endpoint de metadatos (forma de respuesta: key + label/descripción legible) para que el
    //  agregador lo consuma de forma uniforme en todos los MS.

    private ResourceIds() {}
}

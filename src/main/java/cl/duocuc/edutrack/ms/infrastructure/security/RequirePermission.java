package cl.duocuc.edutrack.ms.infrastructure.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declara que un endpoint JAX-RS exige, sobre un recurso identificado, al
 * menos un bit de permiso del modelo Unix-style. La anotación participa en JAX-RS
 * como <i>name binding</i> ({@link NameBinding}): solo se aplica el filtro de
 * autorización a los endpoints que la portan directamente o que están en una
 * clase que la porta.
 *
 * <h3>Cómo se aplica</h3>
 * <p>{@link RequirePermissionFilter} se ejecuta antes de la deserialización del
 * body y de Bean Validation (prioridad {@code AUTHORIZATION}). Para cada
 * request:</p>
 * <ol>
 *   <li>Busca la anotación primero en el método del recurso y, si no está, en
 *       la clase del recurso.</li>
 *   <li>Si {@link #selfParam()} está definido y la identidad propagada por el
 *       Gateway coincide con el valor del path-param indicado, el request se
 *       deja pasar sin consultar permisos.</li>
 *   <li>En caso contrario, delega a {@link PermissionEvaluator} con los
 *       roleIds del request, la clave estable de {@link #resource()} y el
 *       bit de {@link #value()}.</li>
 *   <li>Si el evaluador devuelve {@code false}, aborta el request con
 *       {@code 403 Forbidden} (sin body).</li>
 * </ol>
 *
 * <h3>Restricciones</h3>
 * <ul>
 *   <li>{@link #resource()} es un {@link String} porque las anotaciones Java
 *       solo aceptan constantes de compilación. El valor es una <em>clave
 *       estable</em> del recurso ({@code "<servicio>.<recurso>"}, p. ej.
 *       {@code "auth.users"}), opaca para Auth y comparada por igualdad. Cada MS
 *       define sus constantes de recurso en su propio paquete.</li>
 *   <li>El recurso comodín {@link ResourceIds#ALL} no debe usarse aquí
 *       para anotar endpoints públicos: el comodín existe para grants, no para
 *       contratos de API.</li>
 * </ul>
 *
 * <h3>Ejemplo</h3>
 * <pre>{@code
 * @GET @Path("/{id}")
 * @RequirePermission(resource = AuthResourceId.Key.USERS, value = Permission.READ, selfParam = "id")
 * public UserResponse find(@PathParam("id") UUID id) { ... }
 * }</pre>
 */
@NameBinding
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface RequirePermission {

    /**
     * Clave estable (texto) del recurso sobre el que se evalúa el permiso, p.
     * ej. {@code "auth.users"}; los MS suelen exponer constantes
     * {@code public static final String} por dominio.
     */
    String resource();

    /**
     * Bit mínimo requerido ({@link Permission#READ}, {@link Permission#WRITE}
     * o {@link Permission#EXECUTE}).
     */
    Permission value();

    /**
     * Excepción de "self": nombre del path-param cuyo valor, si coincide con
     * el UUID del usuario propagado por el Gateway, autoriza el acceso sin
     * consultar permisos. Útil para endpoints como {@code GET /users/{id}}
     * donde un usuario siempre puede leer su propio perfil.
     *
     * <p>El path-param se lee con {@code UriInfo.getPathParameters().getFirst(selfParam)}
     * y se compara como string con {@code userId.toString()}.</p>
     *
     * <p>Valor por defecto vacío: sin excepción de self.</p>
     */
    String selfParam() default "";
}

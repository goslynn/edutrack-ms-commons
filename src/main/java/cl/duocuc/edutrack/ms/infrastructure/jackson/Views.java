package cl.duocuc.edutrack.ms.infrastructure.jackson;

/**
 * Jerarquía estándar de vistas Jackson ({@code @JsonView}) compartida por
 * todos los microservicios. Permite modelar la granularidad de campos por
 * endpoint sobre un único par {@code XxxRequest}/{@code XxxResponse} por
 * entidad, sin necesidad de crear DTOs específicos por caso de uso.
 *
 * <h3>Cómo se usa</h3>
 * <p>Cada componente de un record DTO se anota con
 * {@code @JsonView(Views.X.class)} indicando en qué vistas es visible. El
 * endpoint JAX-RS se anota con {@code @JsonView(Views.Y.class)} en el método
 * (para la response) y/o en el parámetro de body (para el request). Por la
 * herencia entre vistas, anotar un endpoint con {@code Views.Detailed} hace
 * visibles también los campos marcados como {@code Views.Base}.</p>
 *
 * <h3>Default-view</h3>
 * <p>{@link JacksonCustomConfig} configura {@link Base} como vista por defecto
 * del {@code ObjectMapper} y deshabilita
 * {@link com.fasterxml.jackson.databind.MapperFeature#DEFAULT_VIEW_INCLUSION}:
 * un campo sin {@code @JsonView} <b>no</b> se serializa ni deserializa. Como
 * consecuencia práctica, los endpoints cuya respuesta es {@code Views.Base} no
 * necesitan anotación; los demás sí.</p>
 *
 * <p>Cada microservicio puede extender esta jerarquía con vistas propias del
 * dominio (p. ej. {@code AuthViews.Login extends Views.Base}). Lo que vive
 * aquí es solo el núcleo transversal.</p>
 */
public interface Views {

    /**
     * Vista mínima de cualquier recurso. Contiene los campos que viajan en
     * todos los endpoints (identificadores, campos esenciales). Es la vista
     * por defecto del {@code ObjectMapper}.
     */
    interface Base {}

    /**
     * Conjunto de campos secundarios que se suman a {@link Base} en vistas
     * "ampliadas" ({@link List}, {@link Patch}, {@link Admin}). Por sí sola
     * no extiende {@code Base}: se compone con ella vía herencia múltiple.
     */
    interface Extra {}

    /**
     * Detalle completo de un recurso (típicamente un {@code GET /{id}}).
     * Extiende {@link Base}.
     */
    interface Detailed extends Base {}

    /**
     * Payload de creación ({@code POST}). Por convención, los campos de
     * presencia obligatoria solo en creación se anotan con
     * {@code @NotNull(groups = Validations.OnCreate.class)}. Extiende
     * {@link Base}.
     */
    interface Create extends Base {}

    /**
     * Listado/paginación: campos visibles en colecciones. Combina {@link Base}
     * y {@link Extra}.
     */
    interface List extends Base, Extra {}

    /**
     * Payload de actualización parcial ({@code PATCH}). Combina {@link Base}
     * y {@link Extra}.
     */
    interface Patch extends Base, Extra {}

    /**
     * Payload de reemplazo total ({@code PUT}). Extiende {@link Base}.
     */
    interface Update extends Base {}

    /**
     * Vista administrativa: campos sensibles que solo se exponen a roles
     * privilegiados. Combina {@link Base} y {@link Extra}.
     */
    interface Admin extends Base, Extra {}

    /**
     * Vista para comunicación entre servicios (no expuesta al cliente final).
     * No extiende {@link Base}: define explícitamente su propio conjunto de
     * campos.
     */
    interface Internal {}
}

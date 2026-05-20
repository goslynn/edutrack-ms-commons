package cl.duocuc.edutrack.ms.infrastructure.context;

/**
 * Catálogo y única fuente de verdad de los nombres de las cabeceras internas
 * que el API Gateway propaga al microservicio una vez validado el JWT del
 * cliente. Cada entrada del enum asocia un nombre simbólico (el que se usa en
 * el código) a su nombre en el wire (el que viaja en la request HTTP).
 *
 * <p>Ninguna otra clase del proyecto debe referenciar el string
 * {@code "X-..."} directamente: leer estas cabeceras se hace siempre vía
 * {@link RequestContext} → {@link RequestHeaders}, que ya entrega los valores
 * tipados y validados.</p>
 *
 * <p>Para agregar una nueva cabecera interna basta con sumar una entrada aquí
 * y exponer su valor tipado en {@link RequestHeaders} (el {@link RequestContext}
 * parsea sólo lo que el record declara).</p>
 */
public enum InternalHeader {

    /**
     * UUID del usuario autenticado. Corresponde al claim {@code sub} del JWT
     * que el Gateway validó. Wire: {@value #USER_ID_WIRE}.
     */
    USER_ID("X-User-Id"),

    /**
     * UUIDs de los roles efectivos del usuario, separados por coma (sin
     * espacios obligatorios; {@link RequestContext} hace {@code trim} de cada
     * token y descarta los vacíos). Wire: {@value #USER_ROLES_WIRE}.
     */
    USER_ROLES("X-User-Roles");

    // Solo para Javadoc; el valor de cada constante vive en su propio wire.
    private static final String USER_ID_WIRE = "X-User-Id";
    private static final String USER_ROLES_WIRE = "X-User-Roles";

    /** Nombre que viaja en la request HTTP (caso real del header). */
    public final String wire;

    InternalHeader(String wire) {
        this.wire = wire;
    }
}

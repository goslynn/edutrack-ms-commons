package cl.duocuc.edutrack.ms.infrastructure.context;

/**
 * Política aplicada por {@link RequestContext} cuando una cabecera interna del
 * Gateway viene <i>presente pero malformada</i> (por ejemplo, {@code X-User-Id}
 * con un valor que no es un UUID, o {@code X-User-Roles} con un token que no
 * parsea como UUID).
 *
 * <p>La <b>ausencia</b> de una cabecera nunca dispara esta política: se
 * interpreta como valor vacío ({@link java.util.Optional#empty()} o lista
 * vacía) en cualquier modo.</p>
 *
 * <p>El modo se selecciona con la propiedad
 * {@code edutrack.headers.validation.mode}; el default es {@link #EAGER}.</p>
 */
public enum HeaderValidationMode {

    /**
     * Header malformado ⇒ se aborta el request con {@code 400 Bad Request}
     * (mensaje {@code "Cabecera interna malformada: <wire>"}) y el endpoint no
     * llega a ejecutarse.
     *
     * <p>Es el modo recomendado para producción: si el Gateway propaga datos
     * inválidos, la cadena de confianza está rota y es preferible fallar
     * rápido que ejecutar lógica con identidad parcial.</p>
     */
    EAGER,

    /**
     * Header malformado ⇒ se emite un log a nivel {@code WARN} con el nombre
     * y el valor recibido, y el campo afectado se trata como ausente; el
     * request continúa.
     *
     * <p>Útil durante una migración del Gateway o en entornos donde se
     * prefiere no tumbar tráfico por un header roto. Cada endpoint debe
     * seguir tolerando identidad ausente (los que la requieren disparan
     * {@code 401} explícitamente vía {@link RequestHeaders#requireUserId()}).</p>
     */
    WARN
}

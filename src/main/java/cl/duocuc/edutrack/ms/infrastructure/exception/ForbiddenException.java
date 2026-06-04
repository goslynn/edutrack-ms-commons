package cl.duocuc.edutrack.ms.infrastructure.exception;

/**
 * {@link DomainException} con status {@code 403 Forbidden} preconfigurado.
 *
 * <p>Se usa cuando la operación está prohibida por una regla de dominio que
 * va más allá del modelo Unix-style de permisos (que ya se aplica vía
 * {@link cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission @RequirePermission}
 * y aborta con 403 sin body). Por ejemplo: "no puedes anular tu propia
 * sesión", "este recurso pertenece a otra organización", etc.</p>
 *
 * <pre>{@code
 * throw new ForbiddenException("AUTH.SESSION.SELF_REVOKE_FORBIDDEN", "Cannot revoke your own session");
 * }</pre>
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        this("", message);
    }

    /**
     * @param code    código de dominio estable, formato {@code <MS>.<ENTIDAD>.<CONDICION>}
     * @param message texto legible para el cliente
     */
    public ForbiddenException(String code, String message) {
        super(403, code, message);
    }
}

package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.ws.rs.core.Response;

/**
 * {@link DomainException} con status {@code 403 Forbidden} preconfigurado.
 *
 * <p>Se usa cuando la operación está prohibida por una regla de dominio que
 * va más allá del modelo Unix-style de permisos (que ya se aplica vía
 * {@link cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission @RequirePermission},
 * cuyo filtro lanza esta misma excepción con code
 * {@code "SECURITY.PERMISSION.DENIED"} cuando falla el chequeo de bits). Por
 * ejemplo: "no puedes anular tu propia sesión", "este recurso pertenece a otra
 * organización", etc.</p>
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
        super(Response.Status.FORBIDDEN, code, message);
    }
}

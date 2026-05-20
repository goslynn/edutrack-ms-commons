package cl.duocuc.edutrack.ms.infrastructure.exception;

/**
 * {@link DomainException} con status {@code 404 Not Found} preconfigurado.
 *
 * <p>Se usa cuando el recurso identificado por la URL/parámetros no existe,
 * o cuando el usuario actual no debería poder ver siquiera su existencia
 * (preferir 404 sobre 403 cuando la enumeración de recursos podría ser
 * sensible).</p>
 *
 * <pre>{@code
 * throw new NotFoundException("AUTH.USER.NOT_FOUND", "User not found")
 *     .with("userId", id);
 * }</pre>
 */
public class NotFoundException extends DomainException {

    /**
     * @param code    código de dominio estable, formato {@code <MS>.<ENTIDAD>.<CONDICION>}
     * @param message texto legible para el cliente
     */
    public NotFoundException(String code, String message) {
        super(404, code, message);
    }
}

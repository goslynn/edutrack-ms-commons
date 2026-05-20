package cl.duocuc.edutrack.ms.infrastructure.exception;

/**
 * {@link DomainException} con status {@code 409 Conflict} preconfigurado.
 *
 * <p>Se usa para conflictos de estado del recurso: una invariante de unicidad
 * que ya no se puede cumplir (email duplicado, slug en uso), una transición
 * de estado inválida, una operación que pisaría una versión más nueva, etc.</p>
 *
 * <pre>{@code
 * throw new ConflictException("AUTH.USER.EMAIL_EXISTS", "Email already in use")
 *     .with("email", email);
 * }</pre>
 */
public class ConflictException extends DomainException {

    /**
     * @param code    código de dominio estable, formato {@code <MS>.<ENTIDAD>.<CONDICION>}
     * @param message texto legible para el cliente
     */
    public ConflictException(String code, String message) {
        super(409, code, message);
    }
}

package cl.duocuc.edutrack.ms.infrastructure.persistence.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/**
 * Trait JPA para la columna {@code updated_at}. Las entidades que la usen
 * implementan {@link Holder}; {@link AuditListener} la rellena tanto en
 * {@code @PrePersist} (mismo instante que {@code created_at} si la entidad
 * lo tiene) como en cada {@code @PreUpdate}.
 */
@Embeddable
public class UpdatedAt {

    @Column(name = "updated_at", nullable = false)
    public Instant value;

    /** Marca que la entidad expone una columna {@code updated_at}. */
    public interface Holder {
        UpdatedAt getUpdatedAt();
    }
}

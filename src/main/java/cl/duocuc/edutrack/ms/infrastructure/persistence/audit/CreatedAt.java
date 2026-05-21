package cl.duocuc.edutrack.ms.infrastructure.persistence.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/**
 * Trait JPA para la columna {@code created_at}. Las entidades que la usen
 * implementan {@link Holder} para que {@link AuditListener} la rellene en
 * {@code @PrePersist}.
 */
@Embeddable
public class CreatedAt {

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant value;

    /** Marca que la entidad expone una columna {@code created_at}. */
    public interface Holder {
        CreatedAt getCreatedAt();
    }
}

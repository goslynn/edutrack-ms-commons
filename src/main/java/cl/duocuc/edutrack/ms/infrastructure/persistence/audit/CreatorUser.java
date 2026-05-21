package cl.duocuc.edutrack.ms.infrastructure.persistence.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/**
 * Trait JPA para la columna {@code creator_user}. {@link AuditListener} la
 * rellena en {@code @PrePersist} con el {@code userId} del request actual
 * (cabeceras internas propagadas por el API Gateway).
 */
@Embeddable
public class CreatorUser {

    @Column(name = "creator_user", columnDefinition = "uuid", nullable = false, updatable = false)
    public UUID value;

    /** Marca que la entidad expone una columna {@code creator_user}. */
    public interface Holder {
        CreatorUser getCreatorUser();
    }
}

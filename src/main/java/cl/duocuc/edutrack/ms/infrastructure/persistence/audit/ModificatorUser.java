package cl.duocuc.edutrack.ms.infrastructure.persistence.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/**
 * Trait JPA para la columna {@code modificator_user}. {@link AuditListener} la
 * rellena con el {@code userId} del request actual tanto en {@code @PrePersist}
 * como en {@code @PreUpdate}.
 */
@Embeddable
public class ModificatorUser {

    @Column(name = "modificator_user", columnDefinition = "uuid", nullable = false)
    public UUID value;

    /** Marca que la entidad expone una columna {@code modificator_user}. */
    public interface Holder {
        ModificatorUser getModificatorUser();
    }
}

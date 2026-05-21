package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.CreatedAt;
import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.UpdatedAt;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/** {@code id} + {@code created_at} + {@code updated_at}. Default para entidades mutables. */
@MappedSuperclass
public abstract class TimestampedEntity extends IdentifiedEntity
        implements CreatedAt.Holder, UpdatedAt.Holder {

    @Embedded public CreatedAt createdAt = new CreatedAt();
    @Embedded public UpdatedAt updatedAt = new UpdatedAt();

    @Override public CreatedAt getCreatedAt() { return createdAt; }
    @Override public UpdatedAt getUpdatedAt() { return updatedAt; }
}

package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.UpdatedAt;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/** {@code id} + {@code updated_at}. Caso raro: solo "última modificación", sin fecha de alta. */
@MappedSuperclass
public abstract class UpdatableEntity extends IdentifiedEntity implements UpdatedAt.Holder {

    @Embedded public UpdatedAt updatedAt = new UpdatedAt();

    @Override public UpdatedAt getUpdatedAt() { return updatedAt; }
}

package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.CreatedAt;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/** {@code id} + {@code created_at}. Para entidades append-only (tokens, eventos). */
@MappedSuperclass
public abstract class CreatableEntity extends IdentifiedEntity implements CreatedAt.Holder {

    @Embedded public CreatedAt createdAt = new CreatedAt();

    @Override public CreatedAt getCreatedAt() { return createdAt; }
}

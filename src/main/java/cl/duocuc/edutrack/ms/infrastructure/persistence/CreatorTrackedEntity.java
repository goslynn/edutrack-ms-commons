package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.CreatorUser;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/** {@code id} + {@code creator_user}. Sin timestamps. */
@MappedSuperclass
public abstract class CreatorTrackedEntity extends IdentifiedEntity implements CreatorUser.Holder {

    @Embedded public CreatorUser creatorUser = new CreatorUser();

    @Override public CreatorUser getCreatorUser() { return creatorUser; }
}

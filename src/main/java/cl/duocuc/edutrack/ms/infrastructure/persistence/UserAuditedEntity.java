package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.CreatorUser;
import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.ModificatorUser;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/** {@code id} + {@code creator_user} + {@code modificator_user}. Sin timestamps. */
@MappedSuperclass
public abstract class UserAuditedEntity extends IdentifiedEntity
        implements CreatorUser.Holder, ModificatorUser.Holder {

    @Embedded public CreatorUser creatorUser = new CreatorUser();
    @Embedded public ModificatorUser modificatorUser = new ModificatorUser();

    @Override public CreatorUser getCreatorUser() { return creatorUser; }
    @Override public ModificatorUser getModificatorUser() { return modificatorUser; }
}

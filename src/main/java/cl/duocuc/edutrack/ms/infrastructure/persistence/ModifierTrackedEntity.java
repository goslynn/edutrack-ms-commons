package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.ModificatorUser;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/** {@code id} + {@code modificator_user}. Sin timestamps. */
@MappedSuperclass
public abstract class ModifierTrackedEntity extends IdentifiedEntity implements ModificatorUser.Holder {

    @Embedded public ModificatorUser modificatorUser = new ModificatorUser();

    @Override public ModificatorUser getModificatorUser() { return modificatorUser; }
}

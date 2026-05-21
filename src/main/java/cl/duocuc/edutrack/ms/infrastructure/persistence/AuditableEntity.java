package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.CreatedAt;
import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.CreatorUser;
import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.ModificatorUser;
import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.UpdatedAt;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/**
 * Entidad maestra: {@code id} + los cuatro traits de auditoría
 * ({@code created_at}, {@code updated_at}, {@code creator_user},
 * {@code modificator_user}). Para entidades de dominio críticas donde se
 * necesita la trazabilidad completa de quién creó/modificó y cuándo.
 */
@MappedSuperclass
public abstract class AuditableEntity extends IdentifiedEntity
        implements CreatedAt.Holder, UpdatedAt.Holder, CreatorUser.Holder, ModificatorUser.Holder {

    @Embedded public CreatedAt createdAt = new CreatedAt();
    @Embedded public UpdatedAt updatedAt = new UpdatedAt();
    @Embedded public CreatorUser creatorUser = new CreatorUser();
    @Embedded public ModificatorUser modificatorUser = new ModificatorUser();

    @Override public CreatedAt getCreatedAt() { return createdAt; }
    @Override public UpdatedAt getUpdatedAt() { return updatedAt; }
    @Override public CreatorUser getCreatorUser() { return creatorUser; }
    @Override public ModificatorUser getModificatorUser() { return modificatorUser; }
}

package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.persistence.audit.AuditListener;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/**
 * Raíz de la jerarquía de entidades del monorepo: aporta el {@code id} UUID
 * y enlaza el {@link AuditListener} una sola vez. Todas las combinaciones de
 * auditoría descienden de aquí; los traits opcionales
 * ({@code created_at}, {@code updated_at}, {@code creator_user},
 * {@code modificator_user}) se componen vía {@code @Embedded} en cada
 * subclase, manteniendo el contrato DDL en un único punto por columna.
 */
@MappedSuperclass
@EntityListeners(AuditListener.class)
public abstract class IdentifiedEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;
}

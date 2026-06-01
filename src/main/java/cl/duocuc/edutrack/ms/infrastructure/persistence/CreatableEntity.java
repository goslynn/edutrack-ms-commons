package cl.duocuc.edutrack.ms.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

/**
 * Base <b>append-only</b>: aporta {@code id}, {@code created_at} y
 * {@code creator_user}. Para entidades inmutables (tokens, eventos,
 * snapshots, registros de auditoría) donde una fila se inserta una vez y no
 * se actualiza nunca.
 *
 * <p>Si la entidad puede modificarse después de crearse, extiende
 * {@link AuditableEntity}, que añade {@code updated_at} y
 * {@code updater_user}.</p>
 *
 * <h3>Comportamiento</h3>
 * <ul>
 *   <li>{@link PrePersist}: asigna {@link #createdAt} con
 *       {@link Instant#now()} y {@link #creatorUser} con el {@code userId}
 *       del {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestContext}
 *       actual.</li>
 *   <li>Las columnas son {@code NOT NULL} y {@code updatable = false}: el
 *       DDL impide reescribirlas tras el insert.</li>
 * </ul>
 *
 * <h3>Acceso</h3>
 * <p>Campos {@code public} (Panache active record). Las subclases acceden
 * directamente; código consumidor sigue compilando sin cambios.</p>
 */
@MappedSuperclass
public abstract class CreatableEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "creator_user", columnDefinition = "uuid", nullable = false, updatable = false)
    public UUID creatorUser;

    /**
     * Callback {@link PrePersist}. Las subclases que lo sobreescriban deben
     * llamar a {@code super.onCreate()} (es el patrón usado por
     * {@link AuditableEntity}).
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (creatorUser == null) {
            creatorUser = AuditContext.currentUserOrNoop();
        }
    }
}

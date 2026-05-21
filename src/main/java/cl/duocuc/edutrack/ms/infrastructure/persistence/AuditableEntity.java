package cl.duocuc.edutrack.ms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Base <b>mutable con auditoría completa</b>. Hereda de
 * {@link CreatableEntity} ({@code id}, {@code created_at},
 * {@code creator_user}) y suma {@code updated_at} y {@code updater_user}.
 *
 * <h3>Comportamiento</h3>
 * <ul>
 *   <li>Al persistir: {@link CreatableEntity#onCreate()} fija
 *       {@code createdAt}/{@code creatorUser}; este {@code onCreate()}
 *       (override) los copia a {@code updatedAt}/{@code updaterUser} para
 *       que la fila recién creada quede consistente (ambos pares apuntan al
 *       mismo instante y usuario).</li>
 *   <li>En cada actualización ({@link PreUpdate}): {@code updatedAt} y
 *       {@code updaterUser} se refrescan con la hora actual y el usuario del
 *       request. {@code createdAt}/{@code creatorUser} quedan intactos
 *       ({@code updatable = false} en la superclase).</li>
 * </ul>
 *
 * <h3>Cuándo usarla</h3>
 * <p>Default para entidades mutables del dominio. Solo se prefiere
 * {@link CreatableEntity} cuando la entidad es inmutable por diseño — en
 * ese caso {@code updated_at}/{@code updater_user} no aportan información.
 * </p>
 */
@MappedSuperclass
public abstract class AuditableEntity extends CreatableEntity {

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "updater_user", columnDefinition = "uuid", nullable = false)
    public UUID updaterUser;

    @Override
    protected void onCreate() {
        super.onCreate();
        updatedAt = createdAt;
        updaterUser = creatorUser;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        updaterUser = AuditContext.currentUserOrNull();
    }
}

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
 * Superclase JPA para entidades <b>creables pero inmutables</b>: aporta el
 * {@code id} UUID generado por la aplicación y el timestamp de creación, sin
 * agregar {@code updatedAt}. Pensada para entidades que se insertan una vez y
 * no se modifican después (tokens emitidos, eventos auditados, snapshots,
 * registros append-only).
 *
 * <p>Si la entidad sí puede modificarse, debe extender
 * {@link AuditableEntity}, que añade {@code updatedAt} y su callback
 * {@code @PreUpdate}.</p>
 *
 * <h3>Columnas mapeadas</h3>
 * <ul>
 *   <li>{@code id UUID PRIMARY KEY} — generado por JPA con
 *       {@link GenerationType#UUID}; no modificable
 *       ({@code updatable = false}).</li>
 *   <li>{@code created_at TIMESTAMP NOT NULL} — se asigna en
 *       {@link PrePersist} con {@link Instant#now()}; no modificable.</li>
 * </ul>
 *
 * <h3>Acceso</h3>
 * <p>Los campos son {@code public} siguiendo la convención de Panache Active
 * Record: las subclases acceden a ellos directamente y Panache se encarga de
 * la encapsulación al generar getters/setters en tiempo de compilación.</p>
 *
 * <h3>Extensión</h3>
 * <p>Las subclases que necesiten una segunda fase de inicialización en
 * {@code @PrePersist} deben sobreescribir {@link #onCreate()} y llamar a
 * {@code super.onCreate()}. Es el patrón usado por {@link AuditableEntity}.</p>
 */
@MappedSuperclass
public abstract class CreatableEntity extends PanacheEntityBase {

    /**
     * PK UUID. Se genera al persistir (estrategia
     * {@link GenerationType#UUID}); el código de aplicación normalmente no lo
     * asigna manualmente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;

    /** Instante de creación; se asigna automáticamente en {@link #onCreate()}. */
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /**
     * Callback {@link PrePersist}: asigna {@link #createdAt} con la hora
     * actual. Las subclases deben llamar a {@code super.onCreate()} si
     * sobreescriben este método.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

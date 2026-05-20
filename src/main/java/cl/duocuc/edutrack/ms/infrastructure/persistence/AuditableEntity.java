package cl.duocuc.edutrack.ms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

/**
 * Superclase JPA para entidades <b>mutables con auditoría básica</b>. Hereda
 * de {@link CreatableEntity} ({@code id} UUID + {@code createdAt}) y suma
 * {@code updatedAt}.
 *
 * <h3>Comportamiento</h3>
 * <ul>
 *   <li>Al persistir ({@link jakarta.persistence.PrePersist}, vía
 *       {@link #onCreate()}), se asignan {@code createdAt} y {@code updatedAt}
 *       con el mismo {@link Instant#now()}. Los dos timestamps coinciden en la
 *       fila recién creada.</li>
 *   <li>En cada actualización ({@link PreUpdate}, vía {@link #onUpdate()}),
 *       {@code updatedAt} se reasigna con {@link Instant#now()}.
 *       {@code createdAt} permanece intacto ({@code updatable = false} en la
 *       superclase).</li>
 * </ul>
 *
 * <h3>Columna mapeada</h3>
 * <p>{@code updated_at TIMESTAMP NOT NULL} (por defecto en el schema del MS).</p>
 *
 * <h3>Cuándo usarla</h3>
 * <p>Es la superclase por defecto para la gran mayoría de entidades. Solo se
 * elige {@link CreatableEntity} cuando la entidad es inmutable por diseño
 * (tokens, eventos, registros append-only): en esos casos {@code updatedAt}
 * sería siempre igual a {@code createdAt} y carece de sentido.</p>
 */
@MappedSuperclass
public abstract class AuditableEntity extends CreatableEntity {

    /**
     * Instante del último update; se asigna automáticamente en
     * {@link #onCreate()} y {@link #onUpdate()}.
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * {@inheritDoc}
     *
     * <p>Llama a {@code super.onCreate()} para asignar {@code createdAt} y
     * luego setea {@code updatedAt} con la misma referencia temporal (ambos
     * comparten el mismo {@link Instant#now()} en la creación).</p>
     */
    @Override
    protected void onCreate() {
        super.onCreate();
        updatedAt = Instant.now();
    }

    /**
     * Callback {@link PreUpdate}: refresca {@code updatedAt} con la hora
     * actual antes de cada {@code UPDATE} disparado por Hibernate.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

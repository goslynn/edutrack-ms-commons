package cl.duocuc.edutrack.ms.infrastructure.persistence.audit;

import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Único punto donde se rellenan los traits de auditoría
 * ({@link CreatedAt}, {@link UpdatedAt}, {@link CreatorUser},
 * {@link ModificatorUser}). El listener se enlaza una sola vez en la raíz
 * de la jerarquía ({@code IdentifiedEntity}); cada combinación habilita los
 * traits que necesita implementando los {@code *.Holder} correspondientes,
 * sin volver a duplicar callbacks ni columnas.
 *
 * <p>El {@code userId} se obtiene del {@link RequestContext} (cabeceras
 * internas del Gateway) vía lookup ArC, porque un {@code EntityListener}
 * JPA no es un bean CDI y no puede recibir {@code @Inject}.</p>
 */
public class AuditListener {

    @PrePersist
    void onPersist(Object entity) {
        Instant now = Instant.now();
        if (entity instanceof CreatedAt.Holder h)        h.getCreatedAt().value = now;
        if (entity instanceof UpdatedAt.Holder h)        h.getUpdatedAt().value = now;
        UUID user = currentUserOrNull();
        if (user != null) {
            if (entity instanceof CreatorUser.Holder h)      h.getCreatorUser().value = user;
            if (entity instanceof ModificatorUser.Holder h)  h.getModificatorUser().value = user;
        }
    }

    @PreUpdate
    void onUpdate(Object entity) {
        if (entity instanceof UpdatedAt.Holder h) h.getUpdatedAt().value = Instant.now();
        UUID user = currentUserOrNull();
        if (user != null && entity instanceof ModificatorUser.Holder h) {
            h.getModificatorUser().value = user;
        }
    }

    private static UUID currentUserOrNull() {
        // El listener puede correr fuera de un request (jobs, tests). Si no
        // hay RequestContext activo o no hay userId propagado, devolvemos
        // null y deja al servicio/repo asignarlo explícitamente si aplica.
        try (InstanceHandle<RequestContext> handle = Arc.container().instance(RequestContext.class)) {
            if (handle == null || !handle.isAvailable()) return null;
            return handle.get().headers().userId().orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}

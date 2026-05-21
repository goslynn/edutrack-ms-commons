package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

import java.util.UUID;

/**
 * Lookup del {@code userId} del request actual para rellenar
 * {@code creator_user} / {@code updater_user} desde los callbacks JPA.
 *
 * <p>Un {@code @MappedSuperclass} no es un bean CDI y no admite
 * {@code @Inject}, por eso se resuelve por ArC en demanda. Si no hay
 * {@link RequestContext} activo (jobs batch, tests sin filtros, listeners
 * disparados fuera de un request HTTP), devuelve {@code null} para que el
 * servicio decida qué hacer (asignar a mano o dejar fallar la
 * restricción {@code NOT NULL}).</p>
 */
final class AuditContext {

    private AuditContext() {}

    static UUID currentUserOrNull() {
        try (InstanceHandle<RequestContext> handle = Arc.container().instance(RequestContext.class)) {
            if (handle == null || !handle.isAvailable()) return null;
            return handle.get().headers().userId().orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}

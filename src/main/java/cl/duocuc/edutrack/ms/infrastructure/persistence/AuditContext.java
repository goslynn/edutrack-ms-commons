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
public final class AuditContext {

    private AuditContext() {}
    //TODO: impl desde .env
    public static final UUID NOOP_USER_ID = UUID.fromString("019e5c76-eaed-72c1-ad2c-c3bd0536d71d");

    static UUID currentUserOrNoop() {
        try (InstanceHandle<RequestContext> handle = Arc.container().instance(RequestContext.class)) {
            if (handle == null || !handle.isAvailable()) return null;
            return handle.get().headers().userId().orElse(NOOP_USER_ID);
        } catch (RuntimeException ignored) {
            return NOOP_USER_ID;
        }
    }
}

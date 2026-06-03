package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    private static volatile Props props;

    public static Props props() {
        if (props == null) {
            props = new Props();
        }
        return props;
    }

    static UUID currentUserOrNoop() {
        final UUID noop = props().noopUserId();
        try (InstanceHandle<RequestContext> handle = Arc.container().instance(RequestContext.class)) {
            if (handle == null || !handle.isAvailable()) return null;
            return handle.get().headers()
                    .userId()
                    .orElse(noop);
        } catch (RuntimeException ignored) {
            return noop;
        }
    }

    public static final class Props {
        @ConfigProperty(name = "edutrack.defaults.noop-user-id")
        UUID noopId;

        public UUID noopUserId() {
            if (noopId == null) {
                noopId = UUID.randomUUID();
            }
            return noopId;
        }
    }

}

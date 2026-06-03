package cl.duocuc.edutrack.ms.infrastructure.persistence;

import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import org.eclipse.microprofile.config.ConfigProvider;

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

    /**
     * Acceso al UUID del usuario NOOP configurado en
     * {@code edutrack.defaults.noop-user-id}. La librería ship un default fijo y
     * compartido en su {@code META-INF/microprofile-config.properties}.
     *
     * <p>{@code Props} no es un bean CDI (se instancia con {@code new}), por lo
     * que {@code @ConfigProperty} no se procesaría: el valor se lee
     * programáticamente vía {@link ConfigProvider}. Se usa {@code getValue} —no
     * un fallback aleatorio— para fallar rápido si el sentinela faltara: un noop
     * distinto por arranque rompería la atribución de auditoría. Con el default
     * de la librería presente, ese fallo nunca debería ocurrir.</p>
     */
    public static final class Props {
        UUID noopId;

        public UUID noopUserId() {
            if (noopId == null) {
                noopId = ConfigProvider.getConfig()
                        .getValue("edutrack.defaults.noop-user-id", UUID.class);
            }
            return noopId;
        }
    }

}
